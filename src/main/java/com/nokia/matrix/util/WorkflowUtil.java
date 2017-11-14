/**
 * 
 */
package com.nokia.matrix.util;

import java.util.Collection;
import java.util.Map;

/**
 * @author 1226211
 *
 */
public class WorkflowUtil {
	
	
	 /**
     * @param collection a collection
     * @return true if the collection is neither null nor empty, otherwise false
     */
    public static boolean isNeitherNullNorEmpty(Collection<?> collection)
    {
        return !isNullOrEmpty(collection);
    }

    /**
     * @param map a map
     * @return true if the map is neither null nor empty, otherwise false
     */
    public static boolean isNeitherNullNorEmpty(Map<?, ?> map)
    {
        return !isNullOrEmpty(map);
    }

    /**
     * @param array an array
     * @return true if the array is neither null nor has length of 0, otherwise false
     */
    public static boolean isNeitherNullNorEmpty(Object[] array)
    {
        return !isNullOrEmpty(array);
    }

    /**
     * @param string a string
     * @return true if the string is not null and has non-whitespace length at least one, else false
     */
    public static boolean isNeitherNullNorEmpty(String string)
    {
        return string != null && string.trim().length() > 0;
    }

    /**
     * @param collection a collection
     * @return true if the collection is null or empty, otherwise false
     */
    public static boolean isNullOrEmpty(Collection<?> collection)
    {
        return (collection == null || collection.isEmpty());
    }

    /**
     * @param map a map
     * @return true if the map is null or empty, otherwise false
     */
    public static boolean isNullOrEmpty(Map<?, ?> map)
    {
        return (map == null || map.isEmpty());
    }

    /**
     * @param array an array of objects
     * @return true if the array is null or has length of 0, otherwise false
     */
    public static boolean isNullOrEmpty(Object[] array)
    {
        return (array == null || array.length == 0);
    }

    /**
     * If - the given String is null - or equal to the empty String - or contains only spaces
     * returns true.
     * 
     * If the String has non-space content, returns false.
     * 
     * Useful for parameter checking in requests and elsewhere.
     * 
     * @param String
     * @return boolean
     * @author Jonathan Brown
     */
    public static boolean isNullOrEmpty(String target)
    {
        if (target == null)
        {
            return true;
        }
        if ("".equals(target.trim()))
        {
            return true;
        }
        return false;
    }
    
    /**
     * @param collection a collection
     * @return true if the collection is null or empty, otherwise false
     */
    public static boolean isNull(Object obj)
    {
        return (obj == null);
    }


}
