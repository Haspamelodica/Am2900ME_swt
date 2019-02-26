package net.maisikoleni.am2900me.logic.microinstr;

public enum _CE_M implements MuIField {
	L,
	H;

	@Override
	public String getFullName() {
		return "Enablebit für das MSR";
	}
}