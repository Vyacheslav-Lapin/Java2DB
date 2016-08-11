package common;

import common.reflect.InvocationHandler;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

public class ReflectUtilsTest {

    @FunctionalInterface
    public interface TestFuncInt {
        String hello(String name);
    }

    @Test
    public void getProxyMakerFor() throws Exception {
        TestFuncInt realObj = name -> String.format("Hello %s!", name);
        TestFuncInt proxyObj = InvocationHandler.getProxyMakerFor(TestFuncInt.class).apply(
                (proxy, method, chain, args) ->
                        method.getName().equals("hello") ?
                                chain.apply(realObj) + " from Proxy!" :
                                chain.apply(realObj));

        assertThat(proxyObj.hello("Duke"), is(realObj.hello("Duke") + " from Proxy!"));
        assertThat(proxyObj.toString(), not(realObj.toString()));
        assertThat(proxyObj.hashCode(), is(System.identityHashCode(proxyObj)));
        assertTrue(proxyObj.equals(proxyObj));
        assertFalse(proxyObj.equals(new Object()));
        assertFalse(proxyObj.equals(null));
    }
}