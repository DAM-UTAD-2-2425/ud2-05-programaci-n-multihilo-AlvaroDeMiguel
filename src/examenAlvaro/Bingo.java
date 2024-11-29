package examenAlvaro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * @author
 * @date 20/11/2015
 */

/**
 * Clase que sirve para crear cada uno de los hilos de los jugadores
 */
class Jugador extends Thread {
	final int TOTAL_CARTON = 5; // Cantidad de números por cartón
	int idJugador; // Identificador del jugador
	Set<Integer> carton; // Para almacenar los números pendientes de acertar
	Bombo bombo;
	static boolean BINGOO = false;

	/**
	 * @param identificador del jugador
	 */
	Jugador(int idJugador, Bombo bombo) {
		this.idJugador = idJugador;
		this.bombo = bombo;
		carton = new HashSet<>();
		while (carton.size() < TOTAL_CARTON) {
			carton.add((int) Math.floor(Math.random() * bombo.TOTAL_BOMBO) + 1);
		}
	}

	/**
	 * Muestra el cartón por pantalla con los números pendientes
	 */
	void imprimeCarton() {
		System.out.print("Pendientes jugador " + idJugador + ": ");
		for (Integer integer : carton) {
			System.out.print(integer + " ");
		}
		System.out.println();
	}

	/**
	 * Tacha el número del cartón en caso de que exista
	 *
	 * @param numero Número a tachar
	 */
	void tacharNum(Integer numero) {
		carton.remove(numero);
	}

	public void run() {
		while (!BINGOO) {
			synchronized (bombo) {
				try {
					while (bombo.ultNumero == null || bombo.esNumeroSacado(idJugador)) {
						bombo.wait();
					}
					if (BINGOO)
						break;

					Integer numero = bombo.ultNumero;
					if (carton.contains(numero)) {
						tacharNum(numero);
						System.out.println("Jugador " + idJugador + " tachó el número: " + numero);
						if (carton.isEmpty()) {
							BINGOO = true;
							System.out.println("Jugador " + idJugador + ": ¡¡BINGOOO!!");
							bombo.notifyAll();
						}
					}
					bombo.marcarNumeroSacado(idJugador);
					imprimeCarton();
					bombo.notifyAll(); // Notifica al presentador para que continúe
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}

/**
 * Clase que sirve para el hilo del presentador
 */
class Presentador extends Thread {
	Bombo bombo;
	String nombre;

	Presentador(Bombo bombo) {
		this.bombo = bombo;
		this.nombre = "Jorge Javier Vazquez";
	}

	public void run() {
		try {
			while (!Jugador.BINGOO) {
				synchronized (bombo) {
					while (!bombo.todosNumerosSacados() && bombo.ultNumero != null) {
						bombo.wait();
					}
					if (Jugador.BINGOO)
						break;

					Integer numero = bombo.sacarNum();
					if (numero == null)
						break;
					System.out.println(this.nombre + " sacó el número: " + numero);
					bombo.imprimirBombo();
					bombo.reiniciarSacados(); // Resetea el estado para el próximo número
					bombo.notifyAll(); // Notifica a los jugadores
					Thread.sleep(1000); // Simula el retraso de 1 segundo
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}

/**
 * Clase que se utiliza para crear el objeto compartido entre todos los hilos
 * del programa
 */
class Bombo {
	final int TOTAL_BOMBO = 10; // Números posibles del bombo
	Set<Integer> bombo; // Para almacenar los valores que van saliendo
	Integer ultNumero; // Último número del bombo
	private final Set<Integer> numSacados = new HashSet<>();
	private int totalJugadores;

	/**
	 * Inicializa vacío el bombo
	 */
	Bombo() {
		bombo = new HashSet<>();
	}

	void setTotalJugadores(int totalJugadores) {
		this.totalJugadores = totalJugadores;
	}

	boolean esNumeroSacado(int jugadorId) {
		return numSacados.contains(jugadorId);
	}

	void marcarNumeroSacado(int jugadorId) {
		numSacados.add(jugadorId);
	}

	boolean todosNumerosSacados() {
		return numSacados.size() == totalJugadores;
	}

	void reiniciarSacados() {
		numSacados.clear();
	}

	/**
	 * @return El número que sale del bombo
	 */
	Integer sacarNum() {
		int cantidadBolas = bombo.size();
		if (cantidadBolas < TOTAL_BOMBO) {
			do {
				ultNumero = (int) Math.floor(Math.random() * TOTAL_BOMBO) + 1;
			} while (!bombo.add(ultNumero));
			return ultNumero;
		} else {
			ultNumero = null; // No quedan más bolas
			return null;
		}
	}

	/**
	 * Muestra todas las bolas que han salido hasta el momento
	 */
	void imprimirBombo() {
		System.out.print("Bolas sacadas hasta el momento: ");
		for (Integer integer : bombo) {
			System.out.print(integer + " ");
		}
		System.out.println();
	}
}

/**
 * Clase principal desde la que se inicializa el juego del Bingo
 */
public class Bingo {
	public static void main(String[] args) {
		Scanner bea = new Scanner(System.in);
		System.out.println("Indique numero de jugadores: ");
		int numJugadores = bea.nextInt();
		bea.close();

		Bombo bombo = new Bombo();
		bombo.setTotalJugadores(numJugadores);

		Presentador presentador = new Presentador(bombo);

		List<Jugador> jugadores = new ArrayList<>();
		for (int i = 1; i <= numJugadores; i++) {
			jugadores.add(new Jugador(i, bombo));
		}

		presentador.start();
		for (Jugador jugador : jugadores) {
			jugador.start();
		}

		try {
			presentador.join();
			for (Jugador jugador : jugadores) {
				jugador.join();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		System.out.println("FIN");
	}
}
