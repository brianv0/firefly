package edu.caltech.ipac.firefly.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.MetaElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.firefly.util.regexp.MatchResult;
import edu.caltech.ipac.firefly.util.regexp.RegExp;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.action.ActionConst;
import edu.caltech.ipac.util.dd.EnumFieldDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Date: Nov 28, 2007
 *
 * @author loi
 * @version $Id: GwtUtil.java,v 1.110 2012/11/14 18:40:31 loi Exp $
 */
public class GwtUtil {

    public static final ImageResource EXCLAMATION= IconCreator.Creator.getInstance().exclamation();
    public static final String LOADING_ICON_URL = GWT.getModuleBaseURL() +"images/gxt/loading.gif";
    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();

    private static PopupPane _debugMsgBoxPopup = null;
    private static PopupPanel _debugMsgPopup = null;
    private static PopupPanel _appendMsgPopup = null;
    private static HTML _debugMsgLabel = null;
    private static TextArea _debugMsgBox = null;
    private static HTML _appendMsgLabel = null;
    private static HideTimer _debugMsgHideTimer = null;
    private static DateTimeFormat timeFormat = DateTimeFormat.getFormat("mm:ss.SS");


    public static String getGwtProperty(String name) {
        final NodeList<com.google.gwt.dom.client.Element> meta = Document.get().getElementsByTagName("meta");

        for (int i = 0; i < meta.getLength(); i++) {
            final MetaElement m = MetaElement.as(meta.getItem(i));
            if (m != null && "gwt:property".equals(m.getName())) {
                String[] kv = m.getContent().split("=", 2);
                if (kv[0] != null && kv[0].equals(name)) {
                    return kv.length > 1 ? kv[1] : "";
                }
            }
        }
        return null;
    }
    
    
    public static ShadowedPanel createShadowTitlePanel(Widget content, String title, String helpId, boolean doTag) {
        Widget titlePanel = null;
        if (title != null) {
            titlePanel = new TitlePanel(title, content, !StringUtils.isEmpty(title));
        }
        ShadowedPanel shadow;
        if (titlePanel == null) {
            shadow = new ShadowedPanel(content);
        } else {
            shadow = new ShadowedPanel(titlePanel);
        }
        if (!StringUtils.isEmpty(helpId)) {
            shadow.setHelpId(helpId);
        }

        return shadow;

    }

    public static ShadowedPanel createShadowTitlePanel(Widget content, String title) {
        return createShadowTitlePanel(content, title, null, false);
    }

    public static HorizontalPanel makeHoriPanel(HasHorizontalAlignment.HorizontalAlignmentConstant halign,
                                                HasVerticalAlignment.VerticalAlignmentConstant valign,
                                                Widget... widgets) {
        HorizontalPanel hp = new HorizontalPanel();
        for (Widget w : widgets) {
            hp.add(w);
        }
        if (halign != null) {
            hp.setHorizontalAlignment(halign);
            hp.setWidth("100%");
        }
        if (valign != null) {
            hp.setVerticalAlignment(valign);
        }
        return hp;
    }

    public static VerticalPanel makeVertPanel(HasHorizontalAlignment.HorizontalAlignmentConstant halign,
                                                HasVerticalAlignment.VerticalAlignmentConstant valign,
                                                Widget... widgets) {
        VerticalPanel vp = new VerticalPanel();
        for (Widget w : widgets) {
            vp.add(w);
        }
        if (halign != null) {
            vp.setHorizontalAlignment(halign);
        }
        if (valign != null) {
            vp.setVerticalAlignment(valign);
        }
        return vp;
    }

    public static Widget centerAlign(Widget w) {
        HorizontalPanel hp = new HorizontalPanel();
        hp.setWidth("100%");
        hp.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        hp.add(w);
        return hp;
    }

    public static Widget middleAlign(Widget w) {
        VerticalPanel p = new VerticalPanel();
        p.setHeight("100%");
        p.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
        p.add(w);
        return p;
    }

    public static Widget rightAlign(Widget w) {
        SimplePanel wrapper = new SimplePanel();
        wrapper.setWidth("100%");
        wrapper.setWidget(w);
        DOM.setElementAttribute(wrapper.getElement(), "align", "right");
        return wrapper;
//        HorizontalPanel hp = new HorizontalPanel();
//        hp.setWidth("100%");
//        hp.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
//        hp.add(w);
//        return hp;
    }

    public static Widget leftRightAlign(Widget[] wLeft, Widget[] wRight) {
        HorizontalPanel hp = new HorizontalPanel();
        hp.setWidth("100%");
        hp.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        HorizontalPanel hpLeft = new HorizontalPanel();
        hpLeft.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        hpLeft.setWidth("100%");
        for (Widget w : wLeft) hpLeft.add(w);
        HorizontalPanel hpRight = new HorizontalPanel();
        hpRight.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        hpRight.setWidth("100%");
        for (Widget w : wRight) hpRight.add(w);

        hp.add(hpLeft);
        hp.add(hpRight);
        return hp;
    }

    /**
     * return true if this component is visible and
     * occupy space
     *
     * @param widget the widget to test
     * @return true if is on the display
     */
    public static boolean isOnDisplay(Widget widget) {
        if (widget != null) {
            boolean val = (widget.getOffsetHeight() * widget.getOffsetWidth() > 0)
                    && isVisible(widget.getElement());
            return val;
        }
        return false;
    }

    /**
     * return true if the given element is visible.  this is based on style attribtues.
     * it is possible that a widget is visible, but does not have width or height.
     *
     * @param elem the element to test
     * @return true if visible, false if not
     */
    public static boolean isVisible(com.google.gwt.dom.client.Element elem) {
        if (isHidden(elem)) {
            return false;
        } else {
            com.google.gwt.dom.client.Element p = elem.getParentElement();
            if (p != null) {
                return isVisible(p);
            } else {
                return true;
            }
        }
    }

    public static native boolean isHidden(com.google.gwt.dom.client.Element elem) /*-{
      return (elem.style.visibility == "hidden") || (elem.style.display == "none");
    }-*/;

    public static void setHidden(Widget w, boolean isHidden) {
        setHidden(w.getElement(),isHidden);
    }

    public static void setHidden(Element e, boolean isHidden) {
        String vs = isHidden ? "hidden" : "visible";
        DOM.setStyleAttribute(e, "visibility", vs);
    }

    public static void showAtCenter(PopupPanel popup) {
        int h = Window.getClientHeight();
        int w = Window.getClientWidth();
        popup.show();
        int offsetx = StringUtils.getInt(DOM.getStyleAttribute(popup.getElement(), "width"), 300);
        int offsety = StringUtils.getInt(DOM.getStyleAttribute(popup.getElement(), "height"), 200);
        int x = (w - offsetx) / 2;
        int y = (h - offsety) / 2;
        popup.setPopupPosition(x, y);
    }


    /**
     * create a check box based on the passed property root.
     * Add the style gwtutil-checkbox to the toggle
     *
     * @param prop the property root.
     *             look for prop+".Name" for text, prop+".ShortDescription" for tip,
     *             and prop+".Selected" for checked
     * @return the check box
     */
    public static CheckBox makeCheckBox(String prop) {
        String name = WebProp.getName(prop);
        String tip = WebProp.getTip(prop);
        boolean selected = WebProp.getSelected(prop);
        return makeCheckBox(name, tip, selected);
    }


    public static CheckBox makeCheckBox(String text,
                                        String tip,
                                        boolean selected) {
        CheckBox cb = new CheckBox();
        cb.addStyleName("gwtutil-checkbox");
        cb.setValue(selected);
        cb.setHTML(text);
        cb.setTitle(tip);
        return cb;
    }

    public static void setFileUploadSize(FileUpload widget, String size) {
        DOM.setElementAttribute(widget.getElement(), "size", size);
    }


    public static Widget makeLinkButton(String prop,
                                        ClickHandler handler) {
        String name = WebProp.getName(prop);
        String tip = WebProp.getTip(prop);
        return makeLinkButton(name, tip, handler);
    }

    public static Label makeLinkButton(String text,
                                       String tip,
                                       ClickHandler handler) {
        final Label link = new Label(text);
        link.setTitle(tip);
        if (handler!=null) link.addClickHandler(handler);
        makeIntoLinkButton(link);
        return link;

    }

    public static Widget makeLinkIcon(String iconUrl, String text,
                                      String tip,
                                      ClickHandler handler) {
        HorizontalPanel hp = new HorizontalPanel();
        Image image = new Image(iconUrl);
        image.setHeight("16px");
        makeIntoLinkButton(image);
        hp.add(image);
        if (!StringUtils.isEmpty(text)) {
            Label label = new Label(text);
            if (tip != null) {
                label.setTitle(tip);
            }
            label.addClickHandler(handler);
            makeIntoLinkButton(label);
            hp.add(GwtUtil.getFiller(3, 1));
            hp.add(label);
        }
        if (tip != null) {
            image.setTitle(tip);
        }
        image.addClickHandler(handler);
        return hp;
    }


    public static void makeIntoLinkButton(final Widget... link ) {

        MouseOverHandler mOver= new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                for(Widget w : link) {
                    w.removeStyleName(_ffCss.highlightText());
                    w.addStyleName(_ffCss.markedText());
                }
            }
        };

        MouseOutHandler mOut= new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                for(Widget w : link) {
                    w.removeStyleName(_ffCss.markedText());
                    w.addStyleName(_ffCss.highlightText());
                }
            }
        };


        for(Widget w : link) {
            w.addStyleName("linkTypeButton");
            w.addStyleName(_ffCss.highlightText());

            if (w instanceof HasAllMouseHandlers) {
                HasAllMouseHandlers ol = (HasAllMouseHandlers) w;
                ol.addMouseOverHandler(mOver);
                ol.addMouseOutHandler(mOut);
            }
        }
    }

    public static Param findParam(List<Param> list, String key) {
        if (list != null) {
            for (Param param : list) {
                if (param.getName().equals(key)) {
                    return param;
                }
            }
        }
        return null;
    }


    public static ImageButton makeImageButton(String prop, ClickHandler handler) {
        String url = WebProp.getIcon(prop);
        String tip = WebProp.getTip(prop);
        return makeImageButton(url, tip, handler);
    }


    public static ImageButton makeImageButton(String imageUrl,
                                         String tip,
                                         ClickHandler handler) {
        return new ImageButton(imageUrl,tip,handler);
    }


    public static ImageButton makeImageButton(Image image,
                                         String tip,
                                         ClickHandler handler) {
        return new ImageButton(image,tip,handler);
    }

    public static ImageButton makeImageButton(Image image,
                                              String tip) {
        return new ImageButton(image,tip,null);
    }

    public static HTML makeFaddedHelp(String s) {
        HTML desc = new HTML(s);
        desc.addStyleName("field-desc");
        desc.addStyleName(_ffCss.fadedText());
        return desc;
    }


    public static Widget makeButton(String prop, ClickHandler handler) {
        String name = WebProp.getName(prop);
        String tip = WebProp.getTip(prop);
        return makeButton(name, tip, handler);
    }


    public static Button makeButton(String text, String tip, ClickHandler handler) {
        Button b = new Button(text, handler);
        b.setTitle(tip);
        b.setStyleName("panel-button");
        return b;
    }

    public static ButtonBase makeFormButton(String text, ClickHandler handler) {
        PushButton b = new PushButton();
        b.addClickHandler(handler);
        b.setHTML(text.replace(" ", "&nbsp;"));
        return b;
    }


    public static Widget makeTextInput(String prop, TextBox textBox) {
        WebAppProperties wap = Application.getInstance().getProperties();
        String name = WebProp.getName(prop);
        String tip = WebProp.getTip(prop);
        String value = wap.getProperty(prop + "." + ActionConst.VALUE, "");
        int length = wap.getIntProperty(prop + "." + ActionConst.LENGTH, 20);
        return makeTextInput(name, tip, value, length, textBox);

    }

    public static Widget makeTextInput(String text,
                                       String tip,
                                       String value,
                                       int length,
                                       TextBox textBox) {
        HorizontalPanel hp = new HorizontalPanel();
        Label label = new Label(text);
        label.setTitle(tip);
        textBox.setText(value);
        textBox.setTitle(tip);
        textBox.setVisibleLength(length);
        hp.add(label);
        hp.add(GwtUtil.getFiller(5, 1));
        hp.add(textBox);
        return hp;
    }

    public static ScreenPt getCenterPos(Widget w) {
        if (w == null) return new ScreenPt(-1, -1);
        int x = w.getOffsetWidth() / 2 + w.getAbsoluteLeft();
        int y = w.getOffsetHeight() / 2 + w.getAbsoluteTop();
        return new ScreenPt(x, y);
    }

    public static int getX(Widget w) {
        WebAssert.argTst(w, "w cannot be null");
        int retval;
        if (w.getParent() != null) {
            retval = w.getAbsoluteLeft() - w.getParent().getAbsoluteLeft();
        } else {
            retval = w.getAbsoluteLeft();
        }
        return retval;
    }

    public static int getY(Widget w) {
        WebAssert.argTst(w, "w cannot be null");
        int retval;
        if (w.getParent() != null) {
            retval = w.getAbsoluteTop() - w.getParent().getAbsoluteTop();
        } else {
            retval = w.getAbsoluteTop();
        }
        return retval;
    }

    public static void showDebugMsg(final String msg) { showDebugMsg(msg,false, 20,20); }
    public static void showDebugMsg(final String msg, int x, int y) { showDebugMsg(msg,false, x,y); }
    public static void showDebugMsg(final String msg, boolean isHtml) { showDebugMsg(msg,isHtml, 20,20); }

    public static void showDebugMsg(final String msg, boolean isHtml, int x, int y) {
        if (_debugMsgPopup == null) {
            _debugMsgPopup = new PopupPanel(false, false);
            _debugMsgLabel = new HTML();
            _debugMsgPopup.setWidget(_debugMsgLabel);
            _debugMsgHideTimer = new HideTimer();
        }
        if (isHtml)_debugMsgLabel.setHTML(msg);
        else       _debugMsgLabel.setText(msg);
        int sx = Window.getScrollLeft();
        int sy = Window.getScrollTop();
        _debugMsgPopup.setPopupPosition(sx + x, sy + y);
        _debugMsgPopup.show();
        _debugMsgHideTimer.cancel();
        _debugMsgHideTimer.schedule(60 * 1000);
    }

    public static void showDebugMsgBox(final String msg) {
        if (_debugMsgBoxPopup == null) {
            _debugMsgBox = new TextArea();
            ScrollPanel wrapper = new ScrollPanel(_debugMsgBox);
            wrapper.setSize("300px", "200px");
            _debugMsgBoxPopup = new PopupPane("Debug", wrapper, false, false);
            _debugMsgBox.setCharacterWidth(2000);
            _debugMsgBox.setVisibleLines(10);
        }
        _debugMsgBox.setText(_debugMsgBox.getText() + "\n" + msg);
        if (!_debugMsgBoxPopup.isVisible()) _debugMsgBoxPopup.show();
        _debugMsgBox.getElement().setScrollTop(_debugMsgBox.getElement().getScrollHeight());
    }


    public static void showScrollingDebugMsg(String msg) {
        showScrollingDebugMsg(msg, null);
    }

    public static void showScrollingDebugMsg(final String msg, final Exception e) {
        if (_appendMsgPopup == null) {
            _appendMsgPopup = new PopupPanel(false, false);
            _appendMsgLabel = new HTML();
            _appendMsgLabel.setWordWrap(false);
            ScrollPanel p = new ScrollPanel(_appendMsgLabel);
            p.setSize("200px", "300px");
            _appendMsgPopup.setWidget(p);
            _debugMsgHideTimer = new HideTimer();
        }

        String time = timeFormat.format(new Date());
        String s = _appendMsgLabel.getHTML();
        if (s == null) s = "";
        s = s + "<br>" + time + "&nbsp;&nbsp;" + msg;

        if (e != null) {
            s += ":&nbsp;" + e.getMessage();
            StackTraceElement[] els = e.getStackTrace();
            for (int i = 0; i < els.length; i++) {
                String pad = "";
                for (int j = 0; j <= i; j++) {
                    pad += "&nbsp;&nbsp;";
                }
                s += "<br>" + pad + els[i];
            }
        }

        _appendMsgLabel.setHTML(s);
        _appendMsgPopup.setPopupPosition(500, 20);
        _appendMsgPopup.show();
        _debugMsgHideTimer.cancel();
        _debugMsgHideTimer.schedule(30 * 1000);
    }

    public static void showValidationError() {
        PopupUtil.showError("Validation Error", "Invalid input. Please put the mouse over <img src=\"" + EXCLAMATION.getURL() + "\" alt=\"the red exclamation mark \'!\'\" /> for details.");
    }

    public static Widget getFiller(int width, int height) {
        Label l = new Label();
        l.setPixelSize(width, height);
        return l;
    }


    public static boolean validateBlank(InputField field) {
        boolean retval = !StringUtils.isEmpty(field.getValue());
        if (!retval) {
            field.forceInvalid(field.getFieldDef().getErrMsg());
        }
        return retval;
    }

    public static boolean validateIntList(InputField field) {
        try {
            StringUtils.convertToArrayInt(field.getValue(), ",");
            return true;
        } catch (Exception e) {
            field.forceInvalid(field.getFieldDef().getErrMsg());
            return false;
        }
    }

    /**
     * Collect key/values from the input fields, passed as argument
     *
     * @param fields list of input fields
     * @return list of key/values (key is the name of a field, value - the field value)
     */
    public static List<Param> getFieldValues(List<InputField> fields) {
        ArrayList<Param> list = new ArrayList<Param>();
        String val;
        for (InputField f : fields) {
            val = f.getValue();
            if (!StringUtils.isEmpty(val)) {
                list.add(new Param(f.getFieldDef().getName(), f.getValue()));
            }
        }
        return list;
    }

    /**
     * Set values of the fields to the provided values, reset field if no value is present
     *
     * @param list   list of key/values (key should match a field name for field value to be set)
     * @param fields list of input fields that should be populated from key/values
     */
    public static void setFieldValues(List<Param> list, List<InputField> fields) {
        for (InputField f : fields) {
            Param param = GwtUtil.findParam(list, f.getFieldDef().getName());
            if (param != null) {
                f.setValue(param.getValue());
            } else {
                f.reset();
            }
        }
    }


    public static MaskPane mask(String msg, Widget widget) {
        return mask(msg, widget, MaskPane.MaskHint.OnComponent);
    }


    public static MaskPane mask(String msg, Widget widget, MaskPane.MaskHint hint) {
        DefaultWorkingWidget working = new DefaultWorkingWidget();
        working.setText(msg);
        MaskPane maskPane = new MaskPane(widget, working, hint);
        maskPane.show();
        return maskPane;
    }


    public static void maskAndExecute(String msg,
                                      Widget widget,
                                      MaskPane.MaskHint hint,
                                      final Runnable runnable) {

        final MaskPane mp = mask(msg, widget, hint);
        mp.show();
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                try {
                    runnable.run();
                } finally {
                    mp.hide();
                }
            }
        });
    }


    public static ListBox createComboBox(EnumFieldDef cols) {
        ListBox box = new ListBox(false);
        for (EnumFieldDef.Item item : cols.getEnumValues()) {
            box.addItem(item.getTitle(), item.getName());
        }
        return box;
    }

    public static void populateComboBox(ListBox box, EnumFieldDef cols) {
        box.clear();
        for (EnumFieldDef.Item item : cols.getEnumValues()) {
            box.addItem(item.getTitle(), item.getName());
        }
    }


    private static class HideTimer extends Timer {
        public void run() {
            cancel();
            if (_debugMsgPopup != null) _debugMsgPopup.hide();
            if (_appendMsgPopup != null) _appendMsgPopup.hide();
        }
    }


    public static Widget getTopParent(Widget w) {
        Widget retval = w;
        if (w != null) {
            while (retval.getParent() != null) {
                retval = retval.getParent();
            }
        }
        return retval;
    }


    public static boolean isHexColor(String text) {
        if (text.length() != 6) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.digit(c, 16) == -1) {
                return false;
            }
        }
        return true;
    }

    public static void setStyle(Widget w, String style, String value) {
        DOM.setStyleAttribute(w.getElement(), style, value);
    }

    public static void setStyles(Widget w, String... s) {
        setStyles(w.getElement(), s);
    }

    public static void setStyles(Element e, String... s) {
        WebAssert.argTst((s.length % 2 == 0), "you have an odd number of string parameters, you must " +
                "pass parameters in pairs: style, value");
        for (int i = 0; (i < s.length - 1); i += 2) {
            DOM.setStyleAttribute(e, s[i], s[i + 1]);
        }
    }


    public static void setStyles(Widget w, Param... params) {
        Element e = w.getElement();
        for (Param param : params) {
            DOM.setStyleAttribute(e, param.getName(), param.getValue());
        }
    }


//====================================================================
//
//====================================================================

    public static native void open(String url, String name, String features) /*-{
      winref = $wnd.open(url, name, features);
      winref.focus()
    }-*/;

    public static native void open(String url, String name) /*-{
      winref = $wnd.open(url, name);
      winref.focus()
    }-*/;

    public static int getIntComputedStyle(UIObject ui, String cssStyle) {
        return getIntVal(getComputedStyle(ui.getElement(), cssStyle));
    }


    public static String getComputedStyle(final com.google.gwt.dom.client.Element oElm, final String strCssRule) {
        String strValue;
        if (BrowserUtil.isIE()) {
            strValue= getComputedStyleIE(oElm,StringUtils.convertDashedToCamel(strCssRule));
        } else {
            strValue= getComputedStyleStandard(oElm,strCssRule);
        }
        return strValue;
    }



    public static native String getComputedStyleStandard(final com.google.gwt.dom.client.Element oElm, final String strCssRule) /*-{
        var strValue = "";
        if($doc.defaultView && $doc.defaultView.getComputedStyle){
//        // use the W3C's method, if it exists
            strValue = $doc.defaultView.getComputedStyle(oElm, "").getPropertyValue(strCssRule);

        } else if(oElm.currentStyle){
//        // Oterwise, try to us IE's method
            strCssRule = strCssRule.replace(/-(w)/g, function (strMatch, p1){
                            return p1.toUpperCase();
                        });
            strValue = oElm.currentStyle[strCssRule];

        }
        return strValue;
    }-*/;



    public static native String getComputedStyleIE(final com.google.gwt.dom.client.Element oElm, final String strCssRule) /*-{
        var strValue = "";
        if(oElm.currentStyle){
            strValue = oElm.currentStyle[strCssRule];
        }
        return strValue;
    }-*/;




    /**
     * returns the number within the string excluding any non-numeric characters
     *
     * @param val the string to convert
     * @return the converted value
     */
    private static int getIntVal(String val) {
        return StringUtils.getInt(val.replaceAll("[^0-9+-]", ""), 0);
    }

    public static int getHorizPadding(UIObject uiObject) {
        return getIntComputedStyle(uiObject, "padding-left") + getIntComputedStyle(uiObject, "padding-right");
    }

    public static int getVertPadding(UIObject uiObject) {
        return getIntComputedStyle(uiObject, "padding-top") + getIntComputedStyle(uiObject, "padding-bottom");
    }

    public static int getHorizMargin(UIObject uiObject) {
        return getIntComputedStyle(uiObject, "margin-left") + getIntComputedStyle(uiObject, "margin-right");
    }

    public static int getVertMargin(UIObject uiObject) {
        return getIntComputedStyle(uiObject, "margin-top") + getIntComputedStyle(uiObject, "margin-bottom");
    }

    public static int getHorizBorder(UIObject uiObject) {
        return getIntComputedStyle(uiObject, "border-left-width") + getIntComputedStyle(uiObject, "border-right-width");
    }

    public static int getVertBorder(UIObject uiObject) {
        return getIntComputedStyle(uiObject, "border-top-width") + getIntComputedStyle(uiObject, "border-bottom-width");
    }

    public static int getOffsetValueY(UIObject uiObject) {
        return getVertPadding(uiObject) + getVertBorder(uiObject) + getVertMargin(uiObject);
    }

    public static int getOffsetValueX(UIObject uiObject) {
        return getHorizPadding(uiObject) + getHorizBorder(uiObject) + getHorizMargin(uiObject);
    }

    public static int getElementHeight(UIObject uiObject) {
        return uiObject.getOffsetHeight() - getVertPadding(uiObject) - getVertBorder(uiObject);
    }

    public static int getElementWidth(UIObject uiObject) {
        return uiObject.getOffsetWidth() - getHorizPadding(uiObject) - getHorizBorder(uiObject);
    }

    public static String getBackgroundColor(UIObject uiObject) {
        return getBackgroundColor(uiObject.getElement());
    }

    public static String getBackgroundColor(com.google.gwt.dom.client.Element e) {
        return getComputedStyle(e, "background-color");
    }

    /**
     * Guess the number of pixel given the String val.
     *
     * @param relTo TODO what goes here?
     * @param val   the css string value
     * @return the guessed size
     */
    public static int guessPixelValue(int relTo, String val) {
        int retval;
        if (val.indexOf("%") >= 0) {
            retval = relTo * getIntVal(val) / 100;
        } else {
            retval = getIntVal(val);
        }
        return retval;
    }

    /**
     * Cause the ScrollPanel to set it scroll bars so the widget it is showing is centered
     *
     * @param sp the scrollPanel
     */
    public static void centerScrollPanel(ScrollPanel sp) {
        Element el = sp.getElement();
        int sw = DOM.getElementPropertyInt(el, "scrollWidth");
        int sh = DOM.getElementPropertyInt(el, "scrollHeight");
        int w = sp.getOffsetWidth();
        int h = sp.getOffsetHeight();
        sp.setScrollPosition((sh - h) / 2);
        sp.setHorizontalScrollPosition((sw - w) / 2);
    }

    public static native void scrollIntoView(com.google.gwt.dom.client.Element scroll, Element e,
                                             boolean scrollVertically, boolean scrollHorizontally) /*-{
        if (!e)
            return;

        var item = e;
        var realOffset = item.offsetTop;
        var realOffsetH = item.offsetLeft;
//        while (item && (item != scroll)) {
//            realOffset += item.offsetTop;
//            realOffsetH += item.offsetLeft;
//            item = item.offsetParent;
//        }
//
        if (scrollVertically) {
            var deltaY = realOffset - scroll.scrollTop;
            if (deltaY < 0 || deltaY > scroll.offsetHeight - e.offsetHeight) {
                scroll.scrollTop = realOffset - scroll.offsetHeight / 2;
            }
//$wnd.alert("realOffset:" + realOffset + " " +
////           "item.offsetTop:" + item.offsetTop + " " +
//           "scroll.offsetHeight:" + scroll.offsetHeight  + " " +
//           "e.offsetHeight:" + e.offsetHeight);
        }
        if (scrollHorizontally) {
            var deltaX = realOffsetH - scroll.scrollLeft;
            if (deltaX < 0 || deltaX > scroll.offsetWidth - e.offsetWidth) {
                scroll.scrollLeft = realOffsetH - scroll.offsetWidth / 2;
            }
        }
    }-*/;


    public static boolean matchesIgCase(String s, String regExp) {
        return matches(s,regExp,true);
    }

    public static boolean matches(String s, String regExp) {
        return matches(s,regExp,false);
    }

    public static boolean matches(String s, String regExp, boolean ignoreCase) {
        if (s==null) return false;
        RegExp re= ignoreCase ? RegExp.compile(regExp,"i") : RegExp.compile(regExp);
        MatchResult result= re.exec(s);
        boolean found= false;
        if (result!=null && result.getGroupCount()>0) {
            for(int i=0; (i<result.getGroupCount());i++ ) {
                if (s.equals(result.getGroup(i))) {
                    found= true;
                    break;
                }
            }
        }

        return found;
    }

//====================================================================


    public static abstract class DefAsync implements RunAsyncCallback {
        public void onFailure(Throwable reason) {
            PopupUtil.showSevereError(reason);
        }

        public abstract void onSuccess();

    }

    public static class DockLayout extends DockLayoutPanel {
        public DockLayout() {
            super(Style.Unit.PX);
        }

        public static double getDockWidgetSize(Widget widget) {
            if (widget != null) {
                LayoutData lo = (LayoutData) widget.getLayoutData();
                if (lo != null) {
                    return lo.size;
                }
            }
            return 0;
        }

        public static void setWidgetChildSize(Widget widget, double size) {
            if (widget == null) return;
            LayoutData lo = (LayoutData) widget.getLayoutData();
            if (lo != null) {
                lo.size = size;
            }
        }

        public static void showWidget(DockLayoutPanel dockPanel, Widget widget) {
            show(widget);
            dockPanel.forceLayout();
        }

        public static void hideWidget(DockLayoutPanel dockPanel, Widget widget) {
            if (hide(widget)) {
                dockPanel.forceLayout();
            }
        }

        public static boolean isHidden(Widget widget) {
            if (widget != null) {
                LayoutData lo = (LayoutData) widget.getLayoutData();
                if (lo != null) {
                    return lo.hidden;
                }
            }
            return true;
        }

        protected static boolean hide(Widget widget) {
            if (widget != null) {
                LayoutData lo = (LayoutData) widget.getLayoutData();
                if (lo != null) {
                    if (!lo.hidden) {
                        lo.hidden = true;
                        lo.oldSize = lo.size;
                        lo.size = 0;

                        handleShowHide(widget, false);

                        return true;
                    }
                }
            }
            return false;
        }

        protected static boolean show(Widget widget) {
            if (widget != null) {
                LayoutData lo = (LayoutData) widget.getLayoutData();
                if (lo != null) {
                    if (lo.hidden) {
                        double s = lo.oldSize > 0 ? lo.oldSize : lo.originalSize;
                        lo.size = s;
                        lo.hidden = false;

                        handleShowHide(widget, true);

                        return true;
                    }
                }
            }
            return false;
        }

        private static void handleShowHide(Widget w, boolean doShow) {
            if (w == null) return;
            if (w instanceof VisibleListener) {
                if (doShow) {
                    ((VisibleListener)w).onShow();
                } else {
                    ((VisibleListener)w).onHide();
                }
            }
            if (w instanceof HasWidgets) {
                HasWidgets containers = (HasWidgets) w;
                for (Iterator<Widget> itr = containers.iterator(); itr.hasNext(); ) {
                    handleShowHide(itr.next(), doShow);
                }
            }
        }

    }





    public static class SplitPanel extends DockLayout {

        public static void hideWidget(DockLayoutPanel splitPanel, Widget widget) {
            if (!isHidden(widget)) {
                hide(widget);
                setSplitterVisible(splitPanel, widget, false);
                splitPanel.forceLayout();
            }
        }

        public static void hideWidget(SplitLayoutPanel splitPanel, Widget widget) {
            hideWidget((DockLayoutPanel)splitPanel,widget);
        }


        public static void showWidget(DockLayoutPanel splitPanel, Widget widget) {
            if (isHidden(widget)) {
                show(widget);
                setSplitterVisible(splitPanel, widget, true);
                splitPanel.forceLayout();
            }
        }

        public static void showWidget(SplitLayoutPanel splitPanel, Widget widget) {
            showWidget((DockLayoutPanel)splitPanel,widget);
        }

        private static void setSplitterVisible(DockLayoutPanel splitPanel, Widget widget, boolean isVisible) {
            int idx = splitPanel.getWidgetIndex(widget);
            Widget splitter = splitPanel.getWidget(idx + 1);  // this should be the splitter
            if (splitter != null) {
                splitter.setVisible(isVisible);
            }
        }
    }




    public static List<Param> parseParams(String paramStr) {
        List<Param> extraParam = Collections.emptyList();
        if (paramStr != null) {
            String sAry[] = paramStr.split(",");
            if (sAry.length > 0) {
                extraParam = new ArrayList<Param>(sAry.length);

                for (String s : sAry) {
                    String paramParts[] = s.split("=", 2);
                    if (paramParts.length == 2) {
                        extraParam.add(new Param(paramParts[0], paramParts[1]));
                    }

                }
            }
        }
        return extraParam;
    }


    public static class ImageButton extends Composite {

        private Image image;
        private FocusPanel fp;

        public ImageButton (String imageUrl, String tip, ClickHandler handler) {
            this(new Image(imageUrl), tip, handler);
        }

        public ImageButton (Image image, String tip, ClickHandler handler) {
            fp = new FocusPanel();
            this.image= image;
            image.setTitle(tip);
            image.addStyleName("imageTypeButton");
            fp.setWidget(image);
            if (handler!=null) fp.addClickHandler(handler);
            initWidget(fp);
        }

        public void setImage(Image im) {
            image= im;
            fp.setWidget(im);
        }

        public HandlerRegistration addClickHandler(ClickHandler handler) {
            return fp.addClickHandler(handler);
        }

    }

    public static String[] split(String s, String pattern, boolean keepDelims, boolean ignoreCase) {
        if (StringUtils.isEmpty(s) || StringUtils.isEmpty(pattern)) return new String[]{s};

        String flg = ignoreCase ?  "i" : "";
        RegExp p = RegExp.compile(pattern, flg);

        ArrayList<String> retval = new ArrayList<String>();
        String str = s;
        MatchResult matcher;
        while ((matcher = p.exec(str)) != null)
        {
            int idx = matcher.getIndex();
            String delim = matcher.getGroup(0);
            if (idx > 0) {
                String v = str.substring(0, idx);
                if (!StringUtils.isEmpty(v)) {
                    retval.add(v.trim());
                }
            }
            if (keepDelims) {
                retval.add(delim);
            }
            str = str.substring(idx + delim.length());
        }
        if (!StringUtils.isEmpty(str)) {
            retval.add(str.trim());
        }
        return retval.toArray(new String[retval.size()]);
    }


}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/