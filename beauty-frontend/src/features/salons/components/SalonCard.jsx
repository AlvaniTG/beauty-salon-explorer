export const SalonCard = ({ salon, onOpenDetails }) => {
	return (
		<div className="salon-card">
			<div className="salon-card-header">
				<h3>{salon.name}</h3>
				{salon.rating > 0 && (
					<span className="rating">
						{salon.rating} <small>({salon.reviews} opinii)</small>
					</span>
				)}
			</div>

			<div className="salon-card-body">
				<p className="address">
					Dzielnica: <strong>{salon.district || "Brak"}</strong>
				</p>

				{salon.priceRange && <p className="price">Ceny: {salon.priceRange}</p>}
			</div>

			<div className="salon-card-footer">
				<button className="details-btn" onClick={() => onOpenDetails(salon)}>
					Zobacz szczegóły
				</button>
			</div>
		</div>
	);
};
