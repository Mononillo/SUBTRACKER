
    create table t_comercio (
        fecha_creacion datetime(6),
        id bigint not null auto_increment,
        nombre varchar(150) not null,
        patron varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table t_conexion_bancaria (
        cuenta_bancaria_id bigint,
        expira_en datetime(6),
        fecha_actualizacion datetime(6),
        fecha_creacion datetime(6),
        id bigint not null auto_increment,
        usuario_id bigint not null,
        id_sesion varchar(255),
        token_acceso TEXT,
        token_refresco TEXT,
        primary key (id)
    ) engine=InnoDB;

    create table t_cuenta_bancaria (
        fecha_creacion datetime(6),
        id bigint not null auto_increment,
        usuario_id bigint not null,
        iban varchar(34),
        nombre_banco varchar(100),
        uid varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table t_notificacion (
        enviada bit,
        fecha_notificacion date,
        fecha_creacion datetime(6),
        id bigint not null auto_increment,
        usuario_id bigint not null,
        mensaje varchar(255),
        suscripcion_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table t_suscripcion (
        fecha_inicio date,
        importe float(34),
        moneda varchar(3),
        proxima_renovacion date,
        comercio_id bigint,
        fecha_creacion datetime(6),
        usuario_id bigint not null,
        nombre_servicio varchar(100),
        patron_comercio varchar(150),
        id varchar(255) not null,
        confianza ENUM('MUY_BAJA','BAJA','MEDIA','ALTA','MUY_ALTA'),
        estado ENUM('POTENCIAL','ACTIVA','CANCELADA'),
        frecuencia ENUM('MENSUAL','BIMESTRAL','TRIMESTRAL','CUATRIMESTRAL','SEMESTRAL','ANUAL','DESCONOCIDA'),
        primary key (id)
    ) engine=InnoDB;

    create table t_suscripcion_transaccion (
        transaccion_id bigint not null,
        suscripcion_id varchar(255) not null,
        primary key (transaccion_id, suscripcion_id)
    ) engine=InnoDB;

    create table t_transaccion (
        fecha_transaccion date not null,
        importe decimal(10,2) not null,
        moneda varchar(3) not null,
        comercio_id bigint,
        cuenta_bancaria_id bigint not null,
        fecha_registro datetime(6),
        id bigint not null auto_increment,
        id_externo varchar(100),
        comercio varchar(150),
        descripcion varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table t_usuario (
        fecha_registro datetime(6),
        id bigint not null auto_increment,
        nombre varchar(100),
        correo varchar(255) not null,
        hash_contraseña varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table t_comercio 
       add constraint UK7r7rx9lbsr7ptqlf1gs0xwbw7 unique (nombre);

    alter table t_cuenta_bancaria 
       add constraint UK1tcokghdv0f6pw1lga6bqlu2o unique (uid);

    alter table t_transaccion 
       add constraint UKi9l033ld6319q5obuf6t1wofr unique (id_externo);

    alter table t_usuario 
       add constraint UK5cktsg1hpw1iwprjkd25oe8xi unique (correo);

    alter table t_conexion_bancaria 
       add constraint FK2g0vr9glt63pcp6f2q9ivgpra 
       foreign key (cuenta_bancaria_id) 
       references t_cuenta_bancaria (id);

    alter table t_conexion_bancaria 
       add constraint FK2duvwgd83gwuwxhbwe89d14f9 
       foreign key (usuario_id) 
       references t_usuario (id);

    alter table t_cuenta_bancaria 
       add constraint FKb2b8xueaf3yyvwvv8jpp5y1e2 
       foreign key (usuario_id) 
       references t_usuario (id);

    alter table t_notificacion 
       add constraint FKp0ri4niqr0gg79b660w8y6d1g 
       foreign key (suscripcion_id) 
       references t_suscripcion (id);

    alter table t_notificacion 
       add constraint FKa0g72skn8a0photlam6bwbt32 
       foreign key (usuario_id) 
       references t_usuario (id);

    alter table t_suscripcion 
       add constraint FK81vmc7i0ua3plalfwcceohql8 
       foreign key (comercio_id) 
       references t_comercio (id);

    alter table t_suscripcion 
       add constraint FKmsayi2309kuu7tahi3ootr5lc 
       foreign key (usuario_id) 
       references t_usuario (id);

    alter table t_suscripcion_transaccion 
       add constraint FKjgtvaakqakp8am28q31n4oxs 
       foreign key (suscripcion_id) 
       references t_suscripcion (id);

    alter table t_suscripcion_transaccion 
       add constraint FK5u0y2pn3nbwjlcoi0w5mp0ea 
       foreign key (transaccion_id) 
       references t_transaccion (id);

    alter table t_transaccion 
       add constraint FK7sc888fb2llsfk0f067vdtqm2 
       foreign key (comercio_id) 
       references t_comercio (id);

    alter table t_transaccion 
       add constraint FKjdxpe8fuwsg9q44cibm3jhyub 
       foreign key (cuenta_bancaria_id) 
       references t_cuenta_bancaria (id);

    create table t_comercio (
        fecha_creacion datetime(6),
        id bigint not null auto_increment,
        nombre varchar(150) not null,
        patron varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table t_conexion_bancaria (
        cuenta_bancaria_id bigint,
        expira_en datetime(6),
        fecha_actualizacion datetime(6),
        fecha_creacion datetime(6),
        id bigint not null auto_increment,
        usuario_id bigint not null,
        id_sesion varchar(255),
        token_acceso TEXT,
        token_refresco TEXT,
        primary key (id)
    ) engine=InnoDB;

    create table t_cuenta_bancaria (
        fecha_creacion datetime(6),
        id bigint not null auto_increment,
        usuario_id bigint not null,
        iban varchar(34),
        nombre_banco varchar(100),
        uid varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table t_notificacion (
        enviada bit,
        fecha_notificacion date,
        fecha_creacion datetime(6),
        id bigint not null auto_increment,
        usuario_id bigint not null,
        mensaje varchar(255),
        suscripcion_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table t_suscripcion (
        fecha_inicio date,
        importe float(34),
        moneda varchar(3),
        proxima_renovacion date,
        comercio_id bigint,
        fecha_creacion datetime(6),
        usuario_id bigint not null,
        nombre_servicio varchar(100),
        patron_comercio varchar(150),
        id varchar(255) not null,
        confianza ENUM('MUY_BAJA','BAJA','MEDIA','ALTA','MUY_ALTA'),
        estado ENUM('POTENCIAL','ACTIVA','CANCELADA'),
        frecuencia ENUM('MENSUAL','BIMESTRAL','TRIMESTRAL','CUATRIMESTRAL','SEMESTRAL','ANUAL','DESCONOCIDA'),
        primary key (id)
    ) engine=InnoDB;

    create table t_suscripcion_transaccion (
        transaccion_id bigint not null,
        suscripcion_id varchar(255) not null,
        primary key (transaccion_id, suscripcion_id)
    ) engine=InnoDB;

    create table t_transaccion (
        fecha_transaccion date not null,
        importe decimal(10,2) not null,
        moneda varchar(3) not null,
        comercio_id bigint,
        cuenta_bancaria_id bigint not null,
        fecha_registro datetime(6),
        id bigint not null auto_increment,
        id_externo varchar(100),
        comercio varchar(150),
        descripcion varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table t_usuario (
        fecha_registro datetime(6),
        id bigint not null auto_increment,
        nombre varchar(100),
        correo varchar(255) not null,
        hash_contraseña varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table t_comercio 
       add constraint UK7r7rx9lbsr7ptqlf1gs0xwbw7 unique (nombre);

    alter table t_cuenta_bancaria 
       add constraint UK1tcokghdv0f6pw1lga6bqlu2o unique (uid);

    alter table t_transaccion 
       add constraint UKi9l033ld6319q5obuf6t1wofr unique (id_externo);

    alter table t_usuario 
       add constraint UK5cktsg1hpw1iwprjkd25oe8xi unique (correo);

    alter table t_conexion_bancaria 
       add constraint FK2g0vr9glt63pcp6f2q9ivgpra 
       foreign key (cuenta_bancaria_id) 
       references t_cuenta_bancaria (id);

    alter table t_conexion_bancaria 
       add constraint FK2duvwgd83gwuwxhbwe89d14f9 
       foreign key (usuario_id) 
       references t_usuario (id);

    alter table t_cuenta_bancaria 
       add constraint FKb2b8xueaf3yyvwvv8jpp5y1e2 
       foreign key (usuario_id) 
       references t_usuario (id);

    alter table t_notificacion 
       add constraint FKp0ri4niqr0gg79b660w8y6d1g 
       foreign key (suscripcion_id) 
       references t_suscripcion (id);

    alter table t_notificacion 
       add constraint FKa0g72skn8a0photlam6bwbt32 
       foreign key (usuario_id) 
       references t_usuario (id);

    alter table t_suscripcion 
       add constraint FK81vmc7i0ua3plalfwcceohql8 
       foreign key (comercio_id) 
       references t_comercio (id);

    alter table t_suscripcion 
       add constraint FKmsayi2309kuu7tahi3ootr5lc 
       foreign key (usuario_id) 
       references t_usuario (id);

    alter table t_suscripcion_transaccion 
       add constraint FKjgtvaakqakp8am28q31n4oxs 
       foreign key (suscripcion_id) 
       references t_suscripcion (id);

    alter table t_suscripcion_transaccion 
       add constraint FK5u0y2pn3nbwjlcoi0w5mp0ea 
       foreign key (transaccion_id) 
       references t_transaccion (id);

    alter table t_transaccion 
       add constraint FK7sc888fb2llsfk0f067vdtqm2 
       foreign key (comercio_id) 
       references t_comercio (id);

    alter table t_transaccion 
       add constraint FKjdxpe8fuwsg9q44cibm3jhyub 
       foreign key (cuenta_bancaria_id) 
       references t_cuenta_bancaria (id);
