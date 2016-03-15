**Similitude** is a small Java library for deep-copying instances of arbitrary classes. In order to do this, it must also dynamically instantiate arbitrary types, including parameterized types, and perform generic type introspection. These additional capabilities are useful enough by themselves that they are each exposed separately and can be used independently of the cloning functionality.

Generic type introspection is accomplished by using the model of a given object’s class presented by Java’s reflection API to determine where type arguments corresponding to the relevant type parameters may be found at run time, recovering what erasure discards. Also handled are the numerous interactions of generics with other features of Java, such as inheritance and nested classes.

# Example usage
```java
import static org.gdejohn.similitude.TypeToken.typeOf;

import org.gdejohn.similitude.Builder;
import org.gdejohn.similitude.Cloner;
import org.gdejohn.similitude.TypeToken;

import java.util.Collection;
import java.util.LinkedList;

class Demo {
    public static void main(String[] args) {
        TypeToken<Collection<Number>> numberCollection = new TypeToken<Collection<Number>>(){};
        TypeToken<Collection<Double>> doubleCollection = new TypeToken<Collection<Double>>(){};

        LinkedList<Number> list = new LinkedList<Number>();

        list.add(2.5d);
        list.add(9);

        numberCollection.isInstance(list); // true
        doubleCollection.isInstance(list); // false

        Collection<Double> collection = new Builder().instantiate(doubleCollection);
        LinkedList<Number> clonedList = new Cloner().toClone(list);

        TypeToken<? extends LinkedList<Number>> numberList = typeOf(clonedList);

        numberCollection.isAssignableFrom(numberList); // true
    }
}
```
The `TypeToken` instances in this example are constructed using anonymous subclasses to capture the type arguments, as in [Gafter's Gadget](http://gafter.blogspot.com/2006/12/super-type-tokens.html).
