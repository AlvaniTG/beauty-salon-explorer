import { useState } from "react";
import { useSalonDetails } from "../hooks/useSalonDetails";
import { useEditSalon } from "../hooks/useEditSalon";

export const SalonModal = ({ salonId, onClose, onUpdateSuccess }) => {
	const { salon, isLoading, error } = useSalonDetails(salonId);
	const { updateSalon, isSubmitting, submitError } = useEditSalon();

	const [isEditMode, setIsEditMode] = useState(false);
	const [formData, setFormData] = useState({});

	if (isLoading)
		return (
			<div className="modal-overlay" onClick={onClose}>
				<div className="modal-content">Ładowanie szczegółów...</div>
			</div>
		);

	if (error || !salon)
		return (
			<div className="modal-overlay" onClick={onClose}>
				<div className="modal-content">Błąd: {error}</div>
			</div>
		);

	const handleEditClick = () => {
		setFormData({
			name: salon.name || "",
			address: salon.address || "",
			district: salon.district || "",
			phoneNumber: salon.phoneNumber || "",
			priceRange: salon.priceRange || "",
		});
		setIsEditMode(true);
	};

	const handleInputChange = (e) => {
		const { name, value } = e.target;
		setFormData((prev) => ({ ...prev, [name]: value }));
	};

	const handleSave = async () => {
		try {
			const updated = await updateSalon(salonId, formData);

			const savedSalon = { id: salonId, ...updated };

			onUpdateSuccess(savedSalon);
			onClose();
		} catch (err) {
			console.error("Proces zapisu zakończony niepowodzeniem:", err);
		}
	};

	return (
		<div className="modal-overlay" onClick={onClose}>
			<div className="modal-content" onClick={(e) => e.stopPropagation()}>
				<button className="close-btn" onClick={onClose}>
					&times;
				</button>

				<div className="modal-header">
					{isEditMode ? (
						<div className="form-group">
							<label>Nazwa salonu:</label>
							<input
								type="text"
								name="name"
								value={formData.name}
								onChange={handleInputChange}
							/>
						</div>
					) : (
						<h2>{salon.name}</h2>
					)}
				</div>

				<div className="modal-body">
					{/* Wyświetlanie błędu zapisu, jeśli backend odmówi współpracy */}
					{submitError && (
						<div className="error-msg" style={{ color: "red", marginBottom: "15px" }}>
							Błąd zapisu: {submitError}
						</div>
					)}

					<div className="info-group">
						<h4>Lokalizacja</h4>
						{isEditMode ? (
							<>
								<div className="form-group">
									<label>Adres:</label>
									<input
										type="text"
										name="address"
										value={formData.address}
										onChange={handleInputChange}
									/>
								</div>
								<div className="form-group">
									<label>Dzielnica:</label>
									<input
										type="text"
										name="district"
										value={formData.district}
										onChange={handleInputChange}
									/>
								</div>
							</>
						) : (
							<p>
								Adres: {salon.district ? `${salon.district}, ` : ""}
								{salon.address}
							</p>
						)}
					</div>

					<div className="info-group">
						<h4>Kontakt</h4>
						{isEditMode ? (
							<div className="form-group">
								<label>Numer telefonu:</label>
								<input
									type="text"
									name="phoneNumber"
									value={formData.phoneNumber}
									onChange={handleInputChange}
								/>
							</div>
						) : (
							<p>
								{salon.phoneNumber
									? `Telefon: ${salon.phoneNumber}`
									: "Brak numeru telefonu"}
							</p>
						)}
					</div>

					<div className="info-group">
						<h4>Przedział cenowy</h4>
						{isEditMode ? (
							<div className="form-group">
								<label>Ceny:</label>
								<input
									type="text"
									name="priceRange"
									value={formData.priceRange}
									onChange={handleInputChange}
								/>
							</div>
						) : (
							<p>Ceny: {salon.priceRange || "Brak danych"}</p>
						)}
					</div>

					<div className="info-group">
						<h4>Oferowane usługi</h4>
						<div className="services-tags">
							{salon.servicesOffered?.map((service, idx) => (
								<span key={idx} className="badge">
									{service}
								</span>
							))}
						</div>
					</div>
				</div>

				<div className="modal-footer">
					{isEditMode ? (
						<div className="action-buttons">
							<button
								className="cancel-btn"
								onClick={() => setIsEditMode(false)}
								disabled={isSubmitting}
							>
								Anuluj
							</button>
							<button
								className="save-btn"
								onClick={handleSave}
								disabled={isSubmitting}
							>
								{isSubmitting ? "Zapisywanie..." : "Zapisz zmiany"}
							</button>
						</div>
					) : (
						<button className="edit-btn" onClick={handleEditClick}>
							Edytuj dane
						</button>
					)}
				</div>
			</div>
		</div>
	);
};
