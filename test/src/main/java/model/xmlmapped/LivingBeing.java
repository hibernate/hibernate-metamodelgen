package model.xmlmapped;

public class LivingBeing {
	boolean isReallyAlive;

	public boolean isReallyAlive() {
		return isReallyAlive;
	}

	public void setReallyAlive(boolean reallyAlive) {
		isReallyAlive = reallyAlive;
	}

	public int nonPersistent() {
		return 0;
	}
}