package hu.uni_obuda.thesis.railways.data.raildatacollector.util.constant;

import java.util.Map;

public class Constants {

    protected Constants() {

    }

    private static final Map<Character, Character> stationCodeMapping = Map.of(
            'ő', 'õ',
            'ű', 'û',
            'Ő', 'Õ',
            'Ű', 'Û'
    );
}
