package net.maisikoleni.am2900me.logic.microinstr;

public enum _BZ_INC implements MuIField {
	I,
	H;

	@Override
	public String getFullName() {
		return "¬ Befehlszähler Inkrement";
	}
}