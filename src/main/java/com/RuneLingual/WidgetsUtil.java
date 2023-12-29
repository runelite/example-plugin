package com.RuneLingual;

import net.runelite.api.widgets.Widget;

import java.util.List;
import java.util.ArrayList;

public class WidgetsUtil
{
	public static Widget[] getAllWidgets(Widget widget)
	{
		// Check if the widget is not null
		if (widget != null)
		{
			// Create a list to accumulate widgets
			List<Widget> widgetList = new ArrayList<>();
			
			// add the current widget to the list
			widgetList.add(widget);
			
			// get the children of the widget
			Widget[] children = widget.getChildren();
			
			// check if there are children before iterating
			if (children != null)
			{
				// iterate over the children and call the recursive method for each one
				for (Widget child : children)
				{
					// add the widgets from the children to the list
					Widget[] childWidgets = getAllWidgets(child);
					for (Widget childWidget : childWidgets)
					{
						widgetList.add(childWidget);
					}
				}
			}
			
			// convert the list to an array and return
			return widgetList.toArray(new Widget[0]);
		}
		
		// return an empty array if the input widget is null
		return new Widget[0];
	}
}
