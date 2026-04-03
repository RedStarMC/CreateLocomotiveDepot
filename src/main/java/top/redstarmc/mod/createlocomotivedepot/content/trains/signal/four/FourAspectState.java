package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

public enum FourAspectState {

    INVALID,
    RED,
    YELLOW,
    GREEN_YELLOW,
    GREEN;

    public static FourAspectState fromNextState(FourAspectState state) {
        if ( state == null || state == INVALID ) {
            return GREEN;
        }

        return switch ( state ) {
            case RED -> YELLOW;
            case YELLOW -> GREEN_YELLOW;
            case GREEN_YELLOW -> GREEN;
            default -> GREEN;
        };
    }

}
