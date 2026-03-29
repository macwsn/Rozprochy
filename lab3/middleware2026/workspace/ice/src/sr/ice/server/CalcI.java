package sr.ice.server;

import Demo.A;
import Demo.Calc;
import com.zeroc.Ice.Current;
import java.lang.Exception;

public class CalcI implements Calc {
	private static final long serialVersionUID = -2448962912780867770L;
	long counter = 0;

	@Override
	public long add(int a, int b, Current __current) {
		System.out.println("ADD called on object: name=" + __current.id.name + ", category=" + __current.id.category);
		System.out.println("ADD: a = " + a + ", b = " + b + ", result = " + (a + b));

		if (a > 1000 || b > 1000) {
			try {
				Thread.sleep(6000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		if (__current.ctx.values().size() > 0) {
			System.out.println("There are some properties in the context");
		}

		return a + b;
	}

	@Override
	public long subtract(int a, int b, Current __current) {
		System.out.println("SUBTRACT called on object: name=" + __current.id.name + ", category=" + __current.id.category);
		System.out.println("result = " + (a - b));
		return a-b;
	}


	@Override
	public /*synchronized*/ void op(A a1, short b1, Current current) {
		System.out.println("OP called on object: name=" + current.id.name + ", category=" + current.id.category);
		System.out.println("OP" + (++counter));
		try {
			Thread.sleep(500);
		} catch (java.lang.InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public float avg(long[] list, Current __current){
		if (list.length == 0){
			throw new Excpetion();
		}
		float sum = 0;
		for(long val: list){
			sum += val;
		}
		return sum / list.length;
	}
}