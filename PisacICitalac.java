package atomics;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import os.simulation.Application;
import os.simulation.AutoCreate;
import os.simulation.Container;
import os.simulation.Item;
import os.simulation.Operation;
import os.simulation.Thread;

/*
 * Data je zajednicka baza podataka. Vise procesa zeli da istovremeno pristupa
 * ovoj bazi kako bi citali ili upisivali podatke u nju. Kako bi korektno
 * realizovali ove istovremene pristupe bez rizika da dodje do problema,
 * procesi moraju da postuju sledeca pravila: istovremena citanja su dozvoljena
 * posto citaoci ne smetaju jedan drugom, istovremeno citanje i pisanje nije
 * dozvoljeno jer se moze desiti da citalac procita pogresne podatke (do pola
 * upisane), istovremena pisanja takodje nisu dozvoljena jer mogu prouzrokovati
 * ostecenje podataka.
 * 
 * Implementirati sinhronizaciju procesa pisaca i procesa citalaca tako da se
 * postuju opisana pravila.
 */
public class PisacICitalac extends Application {
	
//	AtomicInteger knjiga = new AtomicInteger(0);
	int brojCitalaca = 0;
	Semaphore knjiga = new Semaphore(1);
	Semaphore pise = new Semaphore(1);
	Semaphore cita = new Semaphore(1);

	@AutoCreate(2)
	protected class Pisac extends Thread {

		@Override
		protected void step() {
			radiNestoDrugo();
//			boolean ok = false;
//			do {
//				int k = knjiga.get();
//				ok = k == 0;
//				if(ok) {
//					ok = knjiga.compareAndSet(0, 1);
//				}
//				if(!ok) {
//					Thread.yield();
//				}
//			} while(!ok);
			pise.acquireUninterruptibly();
			knjiga.acquireUninterruptibly();
			pise();
			knjiga.release();
			pise.release();
//			ok = false;
//			do {
//				int k = knjiga.get();
//				ok = k == 1;
//				if(ok) {
//					ok = knjiga.compareAndSet(1, 0);
//				}
//				if(!ok) {
//					Thread.yield();
//				}
//			} while(!ok);
		}
	}

	@AutoCreate(5)
	protected class Citalac extends Thread {

		@Override
		protected void step() {
			radiNestoDrugo();
//			boolean ok = false;
//			do {
//				int k = knjiga.get();
//				ok = k == 0 || k == 2;
//				if(ok && k==0)
//					ok = knjiga.compareAndSet(0, 2);
//				if(ok && k==2) 
//					ok = knjiga.compareAndSet(2, 2);
//				if(!ok)
//					Thread.yield();
//			} while(!ok);
			cita.acquireUninterruptibly();
			if(brojCitalaca == 0)
				knjiga.acquireUninterruptibly();
			brojCitalaca++;
			cita.release();
			cita();
			cita.acquireUninterruptibly();
			brojCitalaca--;
			if(brojCitalaca == 0) 
				knjiga.release();
			cita.release();
//			ok = false;
//			do {
//				int k = knjiga.get();
//				ok = k == 2;
//				if(ok && brojCitalaca == 0)
//					ok = knjiga.compareAndSet(2, 0);
//				else if(ok) 
//					ok = knjiga.compareAndSet(2, 2);
//				if(!ok)
//					Thread.yield();
//			} while(!ok);
		}
	}

	// ------------------- //
	//    Sistemski deo    //
	// ------------------- //
	// Ne dirati kod ispod //
	// ------------------- //

	protected final Container pisci   = box("Писци").color(MAROON);
	protected final Container citaoci = box("Читаоци").color(NAVY);
	protected final Container resurs  = box("База").color(ROYAL);
	protected final Container main    = column(row(pisci, citaoci), resurs);
	protected final Operation pisac   = init().name("Писац %d").color(ROSE).container(pisci);
	protected final Operation citalac = init().name("Читалац %d").color(AZURE).container(citaoci);
	protected final Operation pisanje = duration("7±2").text("Пише").container(resurs).textAfter("Завршио").update(this::azuriraj);;
	protected final Operation citanje = duration("5±2").text("Чита").container(resurs).textAfter("Завршио").update(this::azuriraj);;
	protected final Operation posao   = duration("6±2").text("Ради").textAfter("Чека");

	protected void pise() {
		pisanje.performUninterruptibly();
	}

	protected void cita() {
		citanje.performUninterruptibly();
	}

	protected void radiNestoDrugo() {
		posao.performUninterruptibly();
	}

	protected void azuriraj(Item item) {
		long brP = resurs.stream(Pisac.class).count();
		long brC = resurs.stream(Citalac.class).count();
		resurs.setText(String.format("%d : %d", brP, brC));
		if (brP == 0 && brC == 0) {
			resurs.setColor(NEUTRAL_GRAY);
		} else if (brP > 0 && brC == 0) {
			resurs.setColor(MAROON);
		} else if (brP == 0 && brC > 0) {
			resurs.setColor(NAVY);
		} else {
			resurs.setColor(ROYAL);
		}
	}

	@Override
	protected void initialize() {
		azuriraj(null);
	}

	public static void main(String[] arguments) {
		launch("Писци и читаоци");
	}
}