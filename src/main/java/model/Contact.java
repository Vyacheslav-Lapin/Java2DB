package model;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(fluent = true)
public class Contact implements Serializable {
    private long id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
}