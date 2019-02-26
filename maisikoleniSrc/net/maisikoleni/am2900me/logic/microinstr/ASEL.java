package net.maisikoleni.am2900me.logic.microinstr;

public enum ASEL implements MuIField {
	IR,
	MR;

	@Override
	public String getFullName() {
		return "Register A Adress-Quelle";
	}
}