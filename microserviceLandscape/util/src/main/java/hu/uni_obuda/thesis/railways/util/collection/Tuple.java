package hu.uni_obuda.thesis.railways.util.collection;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class Tuple {

    private Tuple() {

    }

    public static final class Tuple2<A, B> implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final A first;
        private final B second;

        private Tuple2(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public A first() {
            return first;
        }

        public B second() {
            return second;
        }

        public static <A, B> Tuple2<A, B> of(A first, B second) {
            return new Tuple2<>(first, second);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Tuple2<?, ?> other)) return false;
            return Objects.equals(first, other.first)
                    && Objects.equals(second, other.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }

    public static final class Tuple3<A, B, C> implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final A first;
        private final B second;
        private final C third;

        private Tuple3(A first, B second, C third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        public A first() {
            return first;
        }

        public B second() {
            return second;
        }

        public C third() {
            return third;
        }

        public static <A, B, C> Tuple3<A, B, C> of(A first, B second, C third) {
            return new Tuple3<>(first, second, third);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Tuple3<?, ?, ?> other)) return false;
            return Objects.equals(first, other.first)
                    && Objects.equals(second, other.second)
                    && Objects.equals(third, other.third);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second, third);
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ", " + third + ")";
        }
    }

    public static final class Tuple4<A, B, C, D> implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final A first;
        private final B second;
        private final C third;
        private final D fourth;

        private Tuple4(A first, B second, C third, D fourth) {
            this.first = first;
            this.second = second;
            this.third = third;
            this.fourth = fourth;
        }

        public A first() {
            return first;
        }

        public B second() {
            return second;
        }

        public C third() {
            return third;
        }

        public D fourth() {
            return fourth;
        }

        public static <A, B, C, D> Tuple4<A, B, C, D> of(A first, B second, C third, D fourth) {
            return new Tuple4<>(first, second, third, fourth);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Tuple4<?, ?, ?, ?> other)) return false;
            return Objects.equals(first, other.first)
                    && Objects.equals(second, other.second)
                    && Objects.equals(third, other.third)
                    && Objects.equals(fourth, other.fourth);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second, third, fourth);
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ", " + third + ", " + fourth + ")";
        }
    }

    public static <A, B> Tuple2<A, B> of(A first, B second) {
        return Tuple2.of(first, second);
    }

    public static <A, B, C> Tuple3<A, B, C> of(A first, B second, C third) {
        return Tuple3.of(first, second, third);
    }

    public static <A, B, C, D> Tuple4<A, B, C, D> of(A first, B second, C third, D fourth) {
        return Tuple4.of(first, second, third, fourth);
    }
}
