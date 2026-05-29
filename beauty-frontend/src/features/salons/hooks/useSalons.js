import { useState, useEffect } from "react";

export const useSalons = ({ page, size = 12, district, minRating, sort }) => {
	const [data, setData] = useState(null);
	const [isLoading, setIsLoading] = useState(true);
	const [error, setError] = useState(null);

	useEffect(() => {
		const controller = new AbortController();

		const fetchSalons = async () => {
			setIsLoading(true);
			setError(null);

			try {
				// Stabilniejsze budowanie parametrów URL dla Spring Boota
				const params = new URLSearchParams();
				params.append("page", page.toString());
				params.append("size", size.toString());

				if (district) params.append("district", district);
				if (minRating) params.append("minRating", minRating.toString());
				if (sort) params.append("sort", sort);

				const response = await fetch(
					`http://localhost:8080/api/v1/salons?${params.toString()}`,
					{
						signal: controller.signal,
					},
				);

				if (!response.ok) {
					throw new Error(`Serwer odrzucił żądanie. Kod: ${response.status}`);
				}

				const result = await response.json();
				setData(result);
			} catch (err) {
				if (err.name === "AbortError" || err.message.includes("aborted")) {
					return;
				}
				setError(err.message || String(err));
			} finally {
				setIsLoading(false);
			}
		};

		fetchSalons();

		return () => controller.abort();

		// KRYTYCZNE: Tablica zależności. Jeśli tu czegoś brakuje, filtry nie działają.
	}, [page, size, district, minRating, sort]);

	// Zabezpieczona funkcja odświeżania po edycji
	const updateSalonInList = (updatedSalon) => {
		setData((prevData) => {
			if (!prevData || !prevData.content) return prevData;
			return {
				...prevData,
				content: prevData.content.map((item) =>
					item.id === updatedSalon.id ? { ...item, ...updatedSalon } : item,
				),
			};
		});
	};

	return { data, isLoading, error, updateSalonInList };
};
