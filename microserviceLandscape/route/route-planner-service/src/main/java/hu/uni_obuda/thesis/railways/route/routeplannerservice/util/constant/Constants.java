package hu.uni_obuda.thesis.railways.route.routeplannerservice.util.constant;

import java.util.Map;

public class Constants {

    protected Constants() {

    }

    public static final Map<Character, Character> STATION_CODE_MAPPING = Map.of(
            'ő', 'õ',
            'ű', 'û',
            'Ő', 'Õ',
            'Ű', 'Û'
    );
}
