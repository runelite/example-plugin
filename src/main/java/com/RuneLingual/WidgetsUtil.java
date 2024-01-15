package com.RuneLingual;

import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;

import java.util.List;
import java.util.ArrayList;

public class WidgetsUtil
{
	private boolean debugPrints;
	private LogHandler logger;
	public WidgetsUtil()
	{
		this.debugPrints = false;
	}
	
	public static List<Widget> getAllWidgets(Widget widget)
	{
		// Create a list to store widgets
		List<Widget> widgetList = new ArrayList<>();
		
		// Call the recursive method to populate the list
		iterateWidgetsRecursive(widget, widgetList);
		
		// Convert the list to an array and return
		return widgetList;
	}
	
	private static void iterateWidgetsRecursive(Widget widget, List<Widget> widgetList)
	{
		// Check if the widget is not null and not hidden
		if(widget != null && !widget.isHidden())
		{
			// Add the widget to the list
			if(widget.getText().length() > 0 || widget.getName().length() > 0)
			{
				widgetList.add(widget);
				
				//System.out.println("Widget " + widget.getName() + " is not null and was added to list. " + widget.getText());
			}
			
			Widget[] staticChildren = widget.getStaticChildren();
			if (staticChildren != null)
			{
				for (Widget child : staticChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
			Widget[] dynamicChildren = widget.getDynamicChildren();
			if (dynamicChildren != null)
			{
				for (Widget child : dynamicChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
			Widget[] nestedChildren = widget.getNestedChildren();
			if (nestedChildren != null)
			{
				for (Widget child : nestedChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
		}
	}
}
