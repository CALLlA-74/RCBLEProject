package com.example.rcbleproject.Model;

/**
 * Класс BaseParam содержит поля и методы для взаимодействия с параметром соединения порта.
 */
public abstract class BaseParam {
    /**
     * Используется для получения имени параметра.
     * @return название параметра.
     */
    public abstract String getName();

    /**
     * Используется для установления иконки параметра.
     * @return id ресурса иконки параметра.
     */
    public abstract int getIconId();

    /**
     * Используется для замены стандартной иконки меню параметра.
     * @return id ресурса для иконки меню параметра.
     */
    public abstract int getMenuIconId();

    /**
     * Используется для выполнения действия, ассоциированного с параметром.
     * @param obj - объект, необходимый для выполнения действия.
     */
    public abstract void act(Object obj);

    /**
     * Используется для получения состояния флага, уведомляющего о возможности
     * выполнить дейстия метода act().
     * @return флаг активности.
     */
    public abstract boolean getAvailabilityForAct();
}
