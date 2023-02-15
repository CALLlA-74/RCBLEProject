package com.example.rcbleproject;

/**
 * Класс BaseParam содержит поля и методы для взаимодействия с параметром управляемого порта.
 */
public abstract class BaseParam {
    public volatile boolean activeness = true;   // флаг, отражающий активность/неактивность параметра

    /**
     * Возвращает название параметра.
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
    public int getMenuIconId() {
        return R.drawable.baseline_more_vert_20;
    }

    /**
     * Используется для выполнения действия, ассоциированного с параметром.
     * @param obj - объект, необходимый для выполнения действия.
     */
    public void act(Object obj){}
}
