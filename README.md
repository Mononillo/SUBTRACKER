# SUBTRACKER

Aplicación web para gestionar y detectar automáticamente suscripciones de pago recurrente a partir de transacciones bancarias reales mediante Open Banking.

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot 4.0.6 |
| Seguridad | Spring Security + BCrypt |
| Persistencia | Spring Data JPA + Hibernate + MySQL |
| Plantillas | Thymeleaf + Bootstrap 5 |
| Integración bancaria | Enable Banking API (OAuth + JWT RSA-256) |
| Inteligencia artificial | Groq API — Llama 3.1 8B Instant |
| Notificaciones | JavaMailSender + Gmail SMTP |
| Tiempo real | Server-Sent Events (SSE) |
| Build | Maven |

---

## Requisitos previos

- **Java 21** o superior
- **Maven 3.9+**
- **MySQL 8+** accesible en red

---

## Estructura del proyecto

```
subtracker/
├── src/
│   ├── main/
│   │   ├── java/com/subtracker/
│   │   │   ├── config/          # Configuración (BankingProperties, etc.)
│   │   │   ├── controller/      # Controladores MVC
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── mapper/          # Mappers MapStruct
│   │   │   ├── model/           # Entidades JPA
│   │   │   ├── repository/      # Repositorios Spring Data
│   │   │   ├── security/        # Configuración Spring Security
│   │   │   └── service/         # Lógica de negocio
│   │   └── resources/
│   │       ├── static/          # CSS, JS, imágenes
│   │       ├── templates/       # Plantillas Thymeleaf
│   │       ├── keys/            # Clave privada RSA (.pem)
│   │       └── application.properties
│   └── test/
│       └── java/com/subtracker/ # Tests unitarios y de integración
├── create.sql                   # Script de creación del esquema
└── pom.xml
```

---

## Configuración



### application.properties

Usar los valores ya establecidos, únicamente reemplazar la información relacionada con la Base de datos por la información local

Editar `src/main/resources/application.properties` con los valores del entorno:

```properties
# Base de datos
spring.datasource.url=jdbc:mysql://<HOST>:3306/SUBTRACKER?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=<USUARIO>
spring.datasource.password=<CONTRASEÑA>
```

## Ejecución

### Opción A — Maven Wrapper (recomendado)

```bash
./mvnw spring-boot:run
```

### Opción B — Maven instalado

```bash
mvn spring-boot:run
```

### Opción C — JAR ejecutable

```bash
mvn package -DskipTests
java -jar target/subtracker-0.0.1-SNAPSHOT.jar
```

La aplicación arranca en `http://localhost:8080`.

---
---

## Autor

**Jorge Rodríguez Pozo**  
Ciclo Formativo de Grado Superior — Desarrollo de Aplicaciones Multiplataforma  
IES Virgen de la Paloma · 2024-2025
