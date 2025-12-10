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
@EqualsAndHashCode
public class OrderLineId implements Serializable {

    private Integer orderId;
    private Integer productId;
}