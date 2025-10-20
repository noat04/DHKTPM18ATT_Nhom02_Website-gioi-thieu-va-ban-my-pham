// OrderLineId.java
package org.fit.shopnuochoa.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // Cần thiết cho khóa phức hợp
public class OrderLineId implements Serializable {

    private Integer orderId;
    private Integer productId;
}