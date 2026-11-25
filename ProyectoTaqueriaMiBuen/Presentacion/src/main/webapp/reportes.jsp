<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%--
Reporte de ventas.
Total vendido, pedidos cobrados, grafica por dia, semana o mes y productos mas vendidos.
Solo puede entrar el ADMINISTRADOR (lo revisa FiltroSeguridad).
Toda la logica de la pantalla esta en reportes.js.
--%>
<% request.setAttribute("seccion", "reportes");%>
<!DOCTYPE html>
<html lang="es-MX">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Reporte de ventas - Mi Buen</title>
        <link rel="icon" href="assets/logo-mi-buen.png" type="image/png">
        <link rel="stylesheet" href="estilos.css">
    </head>
    <body data-usuario="${sessionScope.usuario.nombre}">
        <%@include file="WEB-INF/partes/encabezado.jspf" %>

        <main class="workspace" id="main">
            <%-- Solo sale al imprimir (flujo 2.2.5) --%>
            <div class="encabezado-impresion" id="encabezadoImpresion"></div>

            <section class="card filtro-reporte" aria-label="Periodo del reporte">
                <h1>Reporte de ventas</h1>
                <div class="rapidos">
                    <button class="chip" type="button" data-rapido="HOY" onclick="elegirRapido('HOY')">Hoy</button>
                    <button class="chip" type="button" data-rapido="SEMANA" onclick="elegirRapido('SEMANA')">Esta semana</button>
                    <button class="chip" type="button" data-rapido="MES" onclick="elegirRapido('MES')">Este mes</button>
                </div>
                <div class="field" id="campoFechas">
                    <label for="fechaInicio">Fechas</label>
                    <div class="fechas">
                        <input id="fechaInicio" type="date" onchange="quitarRapido()">
                        <span>a</span>
                        <input id="fechaFin" type="date" onchange="quitarRapido()">
                    </div>
                </div>
                <div class="field">
                    <label for="agrupar">Agrupar por</label>
                    <select id="agrupar">
                        <option value="DIA">Día</option>
                        <option value="SEMANA">Semana</option>
                        <option value="MES">Mes</option>
                    </select>
                </div>
                <label class="check-comparar"><input id="comparar" type="checkbox"> Comparar con el periodo anterior</label>
                <button class="btn primary" type="button" id="btnGenerar" onclick="generarReporte()">GENERAR REPORTE</button>
            </section>

            <div id="avisoReporte"></div>

            <div class="reporte" id="reporte">
                <div class="card empty"><strong>Elige el periodo</strong>Selecciona las fechas y toca «Generar reporte».</div>
            </div>
        </main>

        <%@include file="WEB-INF/partes/pie.jspf" %>
        <script src="js/comun.js"></script>
        <script src="js/reportes.js"></script>
    </body>
</html>
