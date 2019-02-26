package net.maisikoleni.am2900me.logic.microinstr;

public enum IE implements MuIField {
	IE,
	DIS;

	@Override
	public String getFullName() {
		return "Interrupt Enable";
	}
}