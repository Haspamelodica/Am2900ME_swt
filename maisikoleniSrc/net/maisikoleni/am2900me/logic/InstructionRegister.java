package net.maisikoleni.am2900me.logic;

import net.maisikoleni.am2900me.logic.microinstr._IR_LD;
import net.maisikoleni.am2900me.util.BitUtil;

/**
 * Holds the current machine instruction.
 *
 * @author MaisiKoleni
 *
 */
public class InstructionRegister {
	final InstructionRegisterInput input = new InstructionRegisterInput();
	final InstructionRegisterOutput output = new InstructionRegisterOutput();

	private int instruction;

	/**
	 * Loads the next instruction from the data bus, if _IR_LD is low.
	 * 
	 * @author MaisiKoleni
	 */
	public void process() {
		// assuming no one changes the output :)
		if (input._IR_LD != _IR_LD.L)
			return;
		if (input.data == BitUtil.TRI_STATE_OFF)
			throw new IllegalStateException("cannot write instruction register from data bus, no data signals");
		instruction = input.data;
		output.opCode = instruction >>> 8;
		output.regAAddr = (instruction >>> 4) & 0b0111;
		output.regBAddr = instruction & 0b0111;
	}

	public final int getInstruction() {
		return instruction;
	}

	public final void setInstruction(int instruction) {
		this.instruction = instruction & 0xFFFF;
	}

	public void reset() {
		setInstruction(0);
	}
}

class InstructionRegisterInput {
	_IR_LD _IR_LD;
	int data;
}

class InstructionRegisterOutput {
	int opCode;
	int regAAddr;
	int regBAddr;
}