package com.subtracker.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "T_SUSCRIPCION_TRANSACCION")
public class SuscripcionTransaccion {

	@EmbeddedId
	private SuscripcionTransaccionId id;

	@ManyToOne
	@MapsId("suscripcionId")
	@JoinColumn(name = "suscripcion_id", nullable = false)
	private Suscripcion suscripcion;

	@ManyToOne
	@MapsId("transaccionId")
	@JoinColumn(name = "transaccion_id", nullable = false)
	private Transaccion transaccion;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Embeddable
	public static class SuscripcionTransaccionId implements Serializable {

		private static final long serialVersionUID = 1L;

		@Column(name = "suscripcion_id")
		private String suscripcionId;

		@Column(name = "transaccion_id")
		private Long transaccionId;

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SuscripcionTransaccionId that = (SuscripcionTransaccionId) o;
			return Objects.equals(suscripcionId, that.suscripcionId)
					&& Objects.equals(transaccionId, that.transaccionId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(suscripcionId, transaccionId);
		}
	}
}