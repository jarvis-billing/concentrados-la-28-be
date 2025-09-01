package com.co.jarvis.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class PersonDto implements Serializable {

    private String id;

    @NotEmpty(message = "{empty.persona.tipoDocumento}")
    @Size(message = "{size.persona.tipoDocumento}", max = 10)
    private String documentType;

    @NotEmpty(message = "{empty.persona.numeroDocumento}")
    @Size(message = "{size.persona.numeroDocumento}", max = 10)
    private String documentNumber;

    @NotEmpty(message = "{empty.persona.nombre}")
    @Size(message = "{size.persona.nombre}", max = 100)
    private String name;

    @NotNull(message = "{empty.persona.celular}")
    @Size(message = "{size.persona.nombre}", max = 100)
    private String cellPhone;

    @NotEmpty(message = "{empty.persona.sexo}")
    @Size(message = "{size.persona.sexo}", max = 2)
    private String sex;

    @NotEmpty(message = "{empty.persona.direccion}")
    @Size(message = "{size.persona.direccion}", max = 255)
    private String address;

    @NotEmpty(message = "{empty.persona.correo}")
    @Size(message = "{size.persona.correo}", max = 100)
    private String mail;
}
