package com.qualificamaisanaliticinterface.features.enuns;

public enum Period {

    MORNING(0, "Manhã"), AFTERNOON(1,"Tarde"),
    NIGHT(2, "Noite");

    private Integer code;
    private String description;

    Period(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public static Period toEnum(Integer code) throws IllegalAccessException {
        if(code == null){
            return null;
        }else {
            for (Period p : Period.values()) {
                if (code.equals(p.getCode())) {
                    return p;
                }
            }
        }
        throw new IllegalAccessException("Invalid Priority");
    }
}
