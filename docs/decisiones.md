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
| Login con sesión y cookie (no JWT) | Más simple y seguro por defecto para un solo administrador; la cookie no es legible por JavaScript | JWT: conviene si el frontend vive en otro dominio o si hay una app móvil |
| Cuenta del administrador creada al arrancar desde variables de entorno | No existe un endpoint de registro que atacar, y la contraseña nunca queda en el código ni en GitHub | Endpoint de registro |
| Contraseñas con BCrypt, mínimo 12 caracteres | Se guarda solo el hash; BCrypt es lento a propósito | SHA-256 simple o cifrado reversible |
| Todo denegado por defecto; solo se abre lo permitido explícitamente | Un endpoint olvidado no queda expuesto | Permitir todo y proteger de a uno |
| CSRF activado con `csrf.spa()`, salvo en los endpoints públicos del cliente | Las acciones del administrador usan cookie, y la cookie viaja sola | Desactivar CSRF |
| Confirmar una solicitud rechaza las demás del mismo horario en la misma transacción | Todo o nada; el índice único de la base de datos es la red de seguridad final | Rechazarlas con un proceso aparte |
## API
| Una disponibilidad semanal que se superpone con otra del mismo día se rechaza | Mantiene los datos limpios; dos tramos que solo se tocan sí son válidos | Aceptarlas y que la calculadora elimine los duplicados |
| Un bloqueo no puede pisar una reserva confirmada: se rechaza e indica cuáles chocan | No quedan citas confirmadas en un horario bloqueado | Permitirlo y dejar citas "huérfanas" |
| Eliminar un tramo semanal no toca las reservas ya confirmadas | El tramo solo define qué se ofrece hacia adelante; para anular un día existe el bloqueo | Cancelar en cascada las reservas del tramo |
| Frontend en HTML, CSS y JavaScript sin framework, servido por Spring Boot | Mismo origen que la API: la cookie y el token CSRF funcionan sin CORS, y no hay herramientas de build | React o Vue con Vite en una app aparte |
| Los textos del usuario se insertan con `textContent`, nunca con `innerHTML`, y una política CSP bloquea scripts y estilos de otros orígenes | Doble defensa contra XSS: un nombre como `<script>` se muestra como texto, y aunque se colara algo, el navegador no lo ejecutaría | Confiar solo en validar los datos de entrada |
| `GET /api/horarios/resumen` entrega los días con horas libres en una llamada | El navegador no hace una consulta por cada día | Preguntar día por día desde el frontend |
| La página recuerda los enlaces de las solicitudes en el navegador (`localStorage`) | Un cliente recupera su enlace sin crear cuenta | Exigir una cuenta al cliente |