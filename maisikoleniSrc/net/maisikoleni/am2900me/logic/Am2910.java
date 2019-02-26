package net.maisikoleni.am2900me.logic;

import net.maisikoleni.am2900me.logic.microinstr.Am2910_Inst;
import net.maisikoleni.am2900me.logic.microinstr._CCEN;

/**
 * The Am2910 - the "Microprogram Controller"<br>
 * Determines the address of next microinstruction to execute.
 *
 * @author MaisiKoleni
 *
 */
public class Am2910 {

	private final int[] stack = new int[5];

	private int stackPointer;
	private int registerCounter;
	private int incrementerSignal;
	private int muPC;

	final Am2910input input = new Am2910input();
	final Am2910output output = new Am2910output();

	/**
	 * Sets one of the outputs _PL, _MAP, _VECT, depending on the Am2910 instruction
	 * 
	 * @author MaisiKoleni
	 */
	public void processStep1() {
		muPC = incrementerSignal;
		switch (input.mi_inst) {
		case JZ:
		case CJS:
		case CJP:
		case PUSH:
		case JSRP:
		case JRP:
		case RFCT:
		case RPCT:
		case CRTN:
		case CJPP:
		case LDCT:
		case LOOP:
		case CONT:
		case TWB:
			output._PL = 0;
			output._MAP = 1;
			output._VECT = 1;
			break;
		case JMAP:
			output._PL = 1;
			output._MAP = 0;
			output._VECT = 1;
			break;
		case CJV:
			output._PL = 1;
			output._MAP = 1;
			output._VECT = 0;
			break;
		default:
			throw new IllegalArgumentException("illegal Am2910 instruction: " + input.mi_inst);
		}
	}

	/**
	 * Determines the next microinstruction's address on the Y output
	 * 
	 * @author MaisiKoleni
	 */
	public void processStep2() {
		int passed = input._CCEN.ordinal() | (1 - input._CC);
		// if _RDL low, always load
		if (input._RLD == 0)
			registerCounter = input.D;
		// output is set low the cycle AFTER the stack got filled
		output._FULL = 1 - stackPointer / 5;
		int Y = emulateInstPLA(input.mi_inst, passed == 1, registerCounter == 0);
		incrementerSignal = Y + input.CI;
		output.Y = Y;
	}

	private int emulateInstPLA(Am2910_Inst inst, boolean passed, boolean regCounterZero) {
		switch (inst) {
		case JZ:
			clearStack();
			return 0;
		case CJS:
			if (passed) {
				pushStack();
				return input.D;
			}
			return muPC;
		case JMAP:
			return input.D;
		case CJP:
			if (passed)
				return input.D;
			return muPC;
		case PUSH:
			pushStack();
			if (passed)
				registerCounter = input.D;
			return muPC;
		case JSRP:
			pushStack();
			if (passed)
				return input.D;
			return registerCounter;
		case CJV:
			if (passed)
				registerCounter = input.D;
			return muPC;
		case JRP:
			if (passed)
				registerCounter = input.D;
			return registerCounter;
		case RFCT:
			if (regCounterZero) {
				popStack();
				return muPC;
			}
			return --registerCounter;
		case RPCT:
			if (regCounterZero) {
				return muPC;
			}
			registerCounter--;
			return input.D;
		case CRTN:
			if (passed) {
				popStack();
				return registerCounter;
			}
			return muPC;
		case CJPP:
			if (passed) {
				popStack();
				return input.D;
			}
			return muPC;
		case LDCT:
			registerCounter = input.D;
			return muPC;
		case LOOP:
			if (passed) {
				popStack();
				return muPC;
			}
			return registerCounter;
		case CONT:
			return muPC;
		case TWB:
			if (regCounterZero) {
				popStack();
				return passed ? muPC : input.D;
			}
			registerCounter--;
			if (passed) {
				popStack();
				return muPC;
			}
			return registerCounter;
		default:
			throw new IllegalArgumentException("illegal Am2910 instruction: " + inst);
		}
	}

	/**
	 * Pushes the muPC on the stack. If the stack is full, the top gets overwritten.
	 * 
	 * @author MaisiKoleni
	 */
	private void pushStack() {
		if (stackPointer == 5)
			stack[4] = muPC;
		else
			stack[stackPointer++] = muPC;
	}

	/**
	 * Pops the last muPC from the stack and returns it. If the stack is empty, the
	 * result is undefined
	 * 
	 * @author MaisiKoleni
	 */
	private int popStack() {
		if (stackPointer == 0)
			return stack[0];
		return stack[--stackPointer];
	}

	private void clearStack() {
		stackPointer = 0;
	}

	public final int getRegisterCounter() {
		return registerCounter;
	}

	public final int getmuPC() {
		return muPC;
	}

	public final void setRegisterCounter(int registerCounter) {
		this.registerCounter = registerCounter & 0xFFF;
	}

	public final void setmuPC(int muPC) {
		this.incrementerSignal = muPC & 0xFFF;
		this.muPC = muPC & 0xFFF;
	}

	public final int getStackPointer() {
		return stackPointer;
	}

	public final void setStackPointer(int stackPointer) {
		this.stackPointer = Math.min(5, Math.max(0, stackPointer));
	}

	public final int getStack(int pos) {
		return stack[pos];
	}

	public final void setStack(int pos, int value) {
		stack[pos] = value;
	}

	public void reset() {
		clearStack();
		setmuPC(0);
		setRegisterCounter(0);
	}
}

class Am2910input {
	Am2910_Inst mi_inst;
	/**
	 * When high (PS), {@link #_CC} is ignored and tests always pass
	 */
	_CCEN _CCEN;
	/**
	 * 12 bit direct microprogram address input
	 */
	int D;
	/**
	 * Condition code for branch instructions, usually connected to the Am2904's CT
	 */
	int _CC;
	/**
	 * Carry in for the counter's incrementer, can be set low to repeat
	 * microinstructions
	 */
	int CI;
	/**
	 * Forces loading of register/counter when low
	 */
	int _RLD;
	/**
	 * Output enable for the Y output
	 */
	int _OE;
}

class Am2910output {
	/**
	 * 12 bit address of the next microinstruction
	 */
	int Y;
	/**
	 * Low if stack full (size 5)
	 */
	int _FULL;
	/**
	 * Activates the pipeline register as source for the D input
	 */
	int _PL;
	/**
	 * Activates the mapping PROM / PLA as source for the D input
	 */
	int _MAP;
	/**
	 * Activates the interrupt starting address as source for the D input
	 */
	int _VECT;
}
