import { useState, useEffect } from "react";

export const useSalonDetails = (id) => {
	const [salon, setSalon] = useState(null);
	const [isLoading, setIsLoading] = useState(true);
	const [error, setError] = useState(null);

	useEffect(() => {
		if (!id) return;

		const fetchDetails = async () => {
			setIsLoading(true);
			try {
				const response = await fetch(`http://localhost:8080/api/v1/salons/${id}`);
				if (!response.ok) throw new Error("Nie udało się pobrać szczegółów salonu");

				const data = await response.json();
				setSalon(data);
			} catch (err) {
				setError(err.message);
			} finally {
				setIsLoading(false);
			}
		};

		fetchDetails();
	}, [id]);

	return { salon, isLoading, error, setSalon };
};
