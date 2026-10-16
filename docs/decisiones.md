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
| Horarios libres = disponibilidad − bloqueos − reservas confirmadas | Cada concepto vive en su tabla; "este martes no puedo" no altera la disponibilidad semanal | Editar la disponibilidad semanal para cada excepción |
| `CalculadoraHorarios` es lógica pura, separada del acceso a datos | Se prueba en milisegundos, sin base de datos ni Spring | Mezclar consultas y cálculo en un solo servicio: pruebas lentas y frágiles |
| Reglas de negocio en `application.properties` (`app.reservas.*`) | Cambiar la anticipación o la duración no requiere tocar el código | Números escritos directamente en el código |
| `Clock` inyectado en vez de `LocalDate.now()` directo | Las pruebas pueden fijar "hoy" en cualquier fecha | Depender del reloj real: pruebas que fallan según el día |
| Paquetes por funcionalidad (`horario`, `reserva`) | Lo que cambia junto vive junto | Paquetes por capa (`controller`, `service`...): el código de una funcionalidad queda disperso |
| Disponibilidad y bloqueos se cargan a mano por ahora | Crearlos requiere login: se hace en la etapa 4 junto con la seguridad | Dejar endpoints de escritura sin autenticación |
git checkout -b docs/cierre-etapa-2
| El servidor recalcula los horarios libres y exige que la hora pedida esté en la lista | Nunca se confía en lo que manda el navegador | Confiar en que la pantalla solo ofrece horas válidas |
| Cancelar una solicitud pendiente la deja `CANCELADA` con `cancelada_por = CLIENTE` | No se agrega un estado más | Un estado nuevo (por ejemplo `RETIRADA`) |
| Máximo de 2 solicitudes pendientes por teléfono, normalizado a formato E.164 | Defensa barata contra horas fantasma; el formato único evita saltarse el límite | Verificar el teléfono con código |
| Acceso a una reserva solo con token UUID, sin listados, y la API no devuelve el teléfono | Evita IDOR y expone lo mínimo | IDs numéricos en la URL |