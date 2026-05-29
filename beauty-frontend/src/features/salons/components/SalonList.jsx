import { useState } from "react";
import { useSalons } from "../hooks/useSalons";
import { SalonCard } from "./SalonCard";
import { SalonModal } from "./SalonModal";

const WARSAW_DISTRICTS = [
	"Bemowo",
	"Białołęka",
	"Bielany",
	"Mokotów",
	"Ochota",
	"Praga-Południe",
	"Praga-Północ",
	"Rembertów",
	"Śródmieście",
	"Targówek",
	"Ursus",
	"Ursynów",
	"Wawer",
	"Wesoła",
	"Wilanów",
	"Włochy",
	"Wola",
	"Żoliborz",
];

export const SalonList = () => {
	const [page, setPage] = useState(0);

	// Stany filtrów i sortowania
	const [district, setDistrict] = useState("");
	const [minRating, setMinRating] = useState("");
	const [sort, setSort] = useState("rating,desc"); // Domyślne sortowanie zgodne z API

	const [selectedSalonId, setSelectedSalonId] = useState(null);

	const { data, isLoading, error, updateSalonInList } = useSalons({
		page,
		district,
		minRating,
		sort,
	});

	// Uniwersalna funkcja obsługująca zmianę jakiegokolwiek filtra
	const handleFilterChange = (setter) => (e) => {
		setter(e.target.value);
		setPage(0); // Przy zmianie filtra/sortowania zawsze wracamy na początek listy
	};

	if (isLoading && !data) return <div className="spinner">Ładowanie salonów...</div>;
	if (error) return <div className="error-msg">Błąd: {String(error)}</div>;

	return (
		<div className="salon-container">
			{/* SEKCJA ZAAWANSOWANYCH FILTRÓW I SORTOWANIA */}
			<div
				className="filters-bar"
				style={{ display: "flex", gap: "15px", marginBottom: "25px", flexWrap: "wrap" }}
			>
				<div className="filter-group">
					<label
						style={{
							display: "block",
							fontSize: "12px",
							fontWeight: "bold",
							marginBottom: "5px",
						}}
					>
						Dzielnica
					</label>
					<select
						value={district}
						onChange={handleFilterChange(setDistrict)}
						style={{ padding: "8px", borderRadius: "6px" }}
					>
						<option value="">Wszystkie dzielnice</option>
						{WARSAW_DISTRICTS.map((dist) => (
							<option key={dist} value={dist}>
								{dist}
							</option>
						))}
					</select>
				</div>

				<div className="filter-group">
					<label
						style={{
							display: "block",
							fontSize: "12px",
							fontWeight: "bold",
							marginBottom: "5px",
						}}
					>
						Minimalna Ocena
					</label>
					<select
						value={minRating}
						onChange={handleFilterChange(setMinRating)}
						style={{ padding: "8px", borderRadius: "6px" }}
					>
						<option value="">Wszystkie oceny</option>
						<option value="4.0">Od 4.0 wzwyż</option>
						<option value="4.5">Od 4.5 wzwyż</option>
						<option value="4.8">Od 4.8 wzwyż</option>
						<option value="5.0">Tylko 5.0</option>
					</select>
				</div>

				<div className="filter-group">
					<label
						style={{
							display: "block",
							fontSize: "12px",
							fontWeight: "bold",
							marginBottom: "5px",
						}}
					>
						Sortowanie
					</label>
					<select
						value={sort}
						onChange={handleFilterChange(setSort)}
						style={{ padding: "8px", borderRadius: "6px" }}
					>
						<option value="rating,desc">Najwyżej oceniane</option>
						<option value="name,asc">Nazwa: A - Z</option>
						<option value="name,desc">Nazwa: Z - A</option>
					</select>
				</div>
			</div>

			<div className="salon-grid">
				{data?.content?.map((salon) => (
					<SalonCard
						key={salon.id}
						salon={salon}
						onOpenDetails={() => setSelectedSalonId(salon.id)}
					/>
				))}
			</div>

			{/* Paginacja */}
			<div className="pagination">
				<button
					disabled={data?.page?.number === 0 || !data}
					onClick={() => setPage((p) => p - 1)}
				>
					Poprzednia
				</button>

				<span>
					Strona {data ? data.page.number + 1 : 1} z {data?.page?.totalPages || 1}
				</span>

				<button
					disabled={!data || data.page.number === data.page.totalPages - 1}
					onClick={() => setPage((p) => p + 1)}
				>
					Następna
				</button>
			</div>

			{selectedSalonId && (
				<SalonModal
					salonId={selectedSalonId}
					onClose={() => setSelectedSalonId(null)}
					onUpdateSuccess={updateSalonInList}
				/>
			)}
		</div>
	);
};
