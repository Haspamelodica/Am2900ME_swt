package net.maisikoleni.am2900me.logic.microinstr;

public enum BSEL implements MuIField {
	IR,
	MR;

	@Override
	public String getFullName() {
		return "Register B Adress-Quelle";
	}
}