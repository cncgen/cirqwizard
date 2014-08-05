package org.cirqwizard.settings;

import java.util.Arrays;
import java.util.List;

/**
 * Created by simon on 04/08/14.
 */
public class UserPreference<T>
{
    private String userName;
    private T defaultValue;
    private T value;
    private String units;
    private PreferenceType type;
    private List<T> items;
    private Instantiator instantiator;

    public static interface Instantiator<T>
    {
        public T fromString(String str);
    }

    public UserPreference()
    {
    }

    public UserPreference(String userName, T defaultValue, String units)
    {
        this(userName, defaultValue, units, null);
    }

    public UserPreference(String userName, T defaultValue, String units, PreferenceType type)
    {
        this.userName = userName;
        this.defaultValue = defaultValue;
        this.units = units;
        this.type = type;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public T getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public T getValue()
    {
        return value;
    }

    public void setValue(T value)
    {
        this.value = value;
    }

    public String getUnits()
    {
        return units;
    }

    public void setUnits(String units)
    {
        this.units = units;
    }

    public PreferenceType getType()
    {
        return type;
    }

    public UserPreference<T> setType(PreferenceType type)
    {
        this.type = type;
        return this;
    }

    public List<T> getItems()
    {
        return items;
    }

    public void setItems(List<T> items)
    {
        this.items = items;
    }

    public UserPreference<T> setItems(T... items)
    {
        this.items = Arrays.asList(items);
        return this;
    }

    public Instantiator getInstantiator()
    {
        return instantiator;
    }

    public UserPreference<T> setInstantiator(Instantiator<T> instantiator)
    {
        this.instantiator = instantiator;
        return this;
    }
}
