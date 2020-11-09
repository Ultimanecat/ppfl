package trace;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class GcdTest {

	public static int gcd(int a, int b) {
        int r = 0;
        int c = 5;
        int e = c + 3;
        int d = c + 5;
        d = d + e;
		while (b > 0) {
			r = a % b;
			a = b;
			b = r;
			if(a == 1000){
				d = d +10;
			}
			else{
				d = d +100;
			}
		}
        r = r + 1;
        r = r + d;
        a = a + d;
		return a;
	}

	@Test
	void test() {
		boolean fail = false;
		try{
			gcd(28, 8);
		}
		catch(Exception e){
			fail = true;
		}
		assertFalse(fail, "should not fail");
	}

}
