package DAO;

import Conexion.JPAUtil;
import Enums.EstadoMesa;
import Enums.EstadoPedido;
import Entidades.DatosEntrega;
import Entidades.DetallePedido;
import Entidades.Mesa;
import Entidades.Pedido;
import Entidades.PedidoDomicilio;
import Entidades.PedidoLocal;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de los pedidos y sus detalles.
 *
 * @author Equipo 2
 */
public class PedidoDAO {

    private final MesaDAO mesaDAO = new MesaDAO();

    /**
     * Inserta un pedido nuevo con sus detalles y regresa su id.
     * Si es pedido local y la mesa ya esta ocupada, se rechaza.
     */
    public int insertarPedido(Pedido pedido) {
        return insertarPedido(pedido, false);
    }

    /**
     * Igual que insertarPedido(pedido), pero con cuenta separada se permite
     * abrir otro pedido en una mesa que ya esta OCUPADA.
     */
    public int insertarPedido(Pedido pedido, boolean cuentaSeparada) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();

            if (pedido instanceof PedidoLocal) {
                PedidoLocal local = (PedidoLocal) pedido;
                Mesa mesa = mesaDAO.bloquearParaActualizar(em, local.getMesa().getIdMesa());
                if (!mesa.estaDisponible() && !cuentaSeparada) {
                    throw new IllegalStateException("La mesa " + mesa.getNumero() + " ya esta ocupada");
                }
                if (mesa.estaDisponible()) {
                    mesa.setEstado(EstadoMesa.OCUPADA);
                }
                local.setMesa(mesa);
            }

            int turno = siguienteOrdenCola(em);
            for (DetallePedido detalle : pedido.getDetalles()) {
                detalle.setOrdenColaPedido(turno);
                turno++;
            }

            if (pedido instanceof PedidoDomicilio) {
                // Primero se guarda el pedido para que exista el id_pedido que usa datos_entrega
                PedidoDomicilio domicilio = (PedidoDomicilio) pedido;
                DatosEntrega datos = domicilio.getDatosEntrega();
                domicilio.setDatosEntrega(null);
                em.persist(domicilio);
                em.flush();
                domicilio.setDatosEntrega(datos);
                em.persist(datos);
            } else {
                em.persist(pedido);
            }

            em.getTransaction().commit();
            return pedido.getIdPedido();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo insertar el pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Inserta productos nuevos en pedidos abiertos. Cada detalle debe traer
     * su pedido asignado. Quedan al final de la cola de cocina.
     */
    public void insertarDetalles(List<DetallePedido> detalles) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            int turno = siguienteOrdenCola(em);
            for (DetallePedido detalle : detalles) {
                Pedido pedido = em.find(Pedido.class, detalle.getPedido().getIdPedido());
                if (pedido.getEstado() != EstadoPedido.ABIERTO) {
                    throw new IllegalStateException("Solo se pueden agregar productos a un pedido abierto");
                }
                detalle.setOrdenColaPedido(turno);
                turno++;
                pedido.agregarDetalle(detalle);
                em.persist(detalle);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudieron insertar los detalles: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Siguiente turno libre de la cola de cocina (el mayor + 1).
     */
    public int siguienteOrdenCola() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return siguienteOrdenCola(em);
        } finally {
            em.close();
        }
    }

    /**
     * Busca el pedido y carga sus detalles con sus extras.
     */
    public Pedido buscarPorId(int idPedido) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            Pedido pedido = em.find(Pedido.class, idPedido);
            if (pedido != null) {
                cargarDetalles(pedido);
            }
            return pedido;
        } finally {
            em.close();
        }
    }

    /**
     * Pedido abierto o por cobrar de una mesa. Regresa null si la mesa no tiene.
     */
    public Pedido buscarActivoPorMesa(int idMesa) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<PedidoLocal> lista = em.createQuery(
                    "SELECT p FROM PedidoLocal p WHERE p.mesa.idMesa = :idMesa "
                    + "AND p.estado IN (:abierto, :porCobrar) ORDER BY p.fechaApertura DESC", PedidoLocal.class)
                    .setParameter("idMesa", idMesa)
                    .setParameter("abierto", EstadoPedido.ABIERTO)
                    .setParameter("porCobrar", EstadoPedido.POR_COBRAR)
                    .getResultList();
            if (lista.isEmpty()) {
                return null;
            }
            PedidoLocal pedido = lista.get(0);
            cargarDetalles(pedido);
            return pedido;
        } finally {
            em.close();
        }
    }

    public void actualizarEstado(int idPedido, EstadoPedido estado) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Pedido pedido = em.find(Pedido.class, idPedido);
            pedido.setEstado(estado);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar el pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Cierra el pedido: calcula el total y lo deja POR_COBRAR.
     */
    public Pedido cerrarPedido(int idPedido) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Pedido pedido = em.find(Pedido.class, idPedido);
            cargarDetalles(pedido);
            pedido.cerrar();
            em.getTransaction().commit();
            return pedido;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cerrar el pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Cancela el pedido y guarda el motivo y quien lo autorizo.
     * Si es pedido local, la mesa vuelve a quedar DISPONIBLE.
     */
    public Pedido cancelarPedido(int idPedido, String motivo, String autorizadaPor) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Pedido pedido = em.find(Pedido.class, idPedido);
            pedido.cancelar(motivo, autorizadaPor);
            if (pedido instanceof PedidoLocal) {
                ((PedidoLocal) pedido).getMesa().setEstado(EstadoMesa.DISPONIBLE);
            }
            em.persist(pedido.getCancelacion());
            em.getTransaction().commit();
            return pedido;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cancelar el pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public List<Pedido> listarActivos() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Pedido p WHERE p.estado IN (:abierto, :porCobrar) ORDER BY p.fechaApertura",
                    Pedido.class)
                    .setParameter("abierto", EstadoPedido.ABIERTO)
                    .setParameter("porCobrar", EstadoPedido.POR_COBRAR)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Version que usa la transaccion que ya esta abierta
    private int siguienteOrdenCola(EntityManager em) {
        Integer ultimo = em.createQuery("SELECT MAX(d.ordenColaPedido) FROM DetallePedido d", Integer.class)
                .getSingleResult();
        if (ultimo == null) {
            return 1;
        }
        return ultimo + 1;
    }

    // Lee detalles y extras antes de cerrar el EntityManager para poder usarlos despues
    private void cargarDetalles(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            detalle.getExtras().size();
        }
    }
}
