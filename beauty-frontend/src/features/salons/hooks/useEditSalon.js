import { useState } from "react";

export const useEditSalon = () => {
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [submitError, setSubmitError] = useState(null);

	const updateSalon = async (id, updatedFields) => {
		setIsSubmitting(true);
		setSubmitError(null);

		try {
			const response = await fetch(`http://localhost:8080/api/v1/salons/${id}`, {
				method: "PATCH",
				headers: {
					"Content-Type": "application/json",
				},
				body: JSON.stringify(updatedFields),
			});

			if (!response.ok) {
				throw new Error("Serwer odrzucił żądanie aktualizacji danych");
			}

			const updatedSalon = await response.json();
			setIsSubmitting(false);
			return updatedSalon;
		} catch (err) {
			const msg = err.message || String(err);
			setSubmitError(msg);
			setIsSubmitting(false);
			throw err;
		}
	};

	return { updateSalon, isSubmitting, submitError };
};
