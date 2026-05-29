import "./App.css";
import { SalonList } from "./features/salons/components/SalonList";

function App() {
	return (
		<main className="app-container">
			<header className="app-header">
				<h1>Warsaw Beauty Explorer</h1>
				<p>Znajdź najlepsze salony fryzjerskie i kosmetyczne w Twojej okolicy</p>
			</header>

			<SalonList />
		</main>
	);
}

export default App;
