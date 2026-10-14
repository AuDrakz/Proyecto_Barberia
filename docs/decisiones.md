# Decisiones de diseño

Registro corto de decisiones importantes: qué se eligió, por qué y qué se descartó.

| Decisión | Por qué | Alternativa descartada |
|---|---|---|
| Aplicación web pensada para celular | El cliente solo abre un enlace (por ejemplo desde la bio de Instagram), sin instalar nada | App nativa en Kotlin: demasiada fricción para el cliente |
| Maven | Estructura predecible y fácil de retomar en sesiones cortas | Gradle: más flexible, pero sin ventaja real en un proyecto pequeño |
| PostgreSQL + Flyway | Esquema versionado y restricciones que protegen los datos | Dejar que Hibernate cree las tablas: el esquema no queda versionado |
| Solo `CONFIRMADA` bloquea un horario | Una solicitud falsa no le quita horarios a clientes reales | Que `SOLICITADA` bloquee: permite bloquear horarios con solicitudes falsas |
| El administrador confirma cada solicitud | Es el filtro contra "horas fantasma" sin pedir cuenta ni verificación al cliente | Verificar el teléfono con código: costo y fricción innecesarios por ahora |
| Un usuario administrador, sin tabla `barbero` | Hoy hay un solo barbero (YAGNI) | Entidad `Barbero`: complejidad que hoy no se usa |
| Reagendar queda para la v2 | Es una máquina de estados propia; el MVP funciona cancelando y pidiendo otra hora | Incluirlo en el MVP |
