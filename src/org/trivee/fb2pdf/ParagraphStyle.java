package org.trivee.fb2pdf;

import java.lang.reflect.Type;
import com.google.gson.*;

import com.lowagie.text.Chunk;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;

public class ParagraphStyle
{
    private static final class FontStyleInfo
    {
        private String style;
        private boolean fontBold;
        private boolean fontItalic;

        public FontStyleInfo(String style)
            throws FB2toPDFException
        {
            this.style = style;
            if (style.equalsIgnoreCase("regular"))
            {
                fontBold = false;
                fontItalic = false;
            }
            else if (style.equalsIgnoreCase("bold"))
            {
                fontBold = true;
                fontItalic = false;
            }
            else if (style.equalsIgnoreCase("italic"))
            {
                fontBold = false;
                fontItalic = true;
            }
            else if (style.equalsIgnoreCase("bolditalic"))
            {
                fontBold = true;
                fontItalic = true;
            }
            else
            {
                throw new FB2toPDFException("Invalid style '" + style + "'");
            }
        }

        public boolean isFontBold()
        {
            return fontBold;
        }

        public boolean isFontItalic()
        {
            return fontItalic;
        }
    };

    private static final class FontStyleInfoIO
        implements JsonDeserializer<FontStyleInfo>,JsonSerializer<FontStyleInfo>
    {
        public FontStyleInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            try
            {
                return new FontStyleInfo(json.getAsString());
            }
            catch(FB2toPDFException e)
            {
                throw new JsonParseException(e);
            }
        }

        public JsonElement serialize(FontStyleInfo info, Type typeOfId, JsonSerializationContext context)
        {
            return new JsonPrimitive(info.style);
        }
    }

    private static final class AlignmentInfo
    {
        private String alignment;
        private int alignmentValue;

        public AlignmentInfo(String alignment)
            throws FB2toPDFException
        {
            this.alignment = alignment;
            if (alignment.equalsIgnoreCase("left"))
                this.alignmentValue = Paragraph.ALIGN_LEFT;
            else if (alignment.equalsIgnoreCase("center"))
                this.alignmentValue = Paragraph.ALIGN_CENTER;
            else if (alignment.equalsIgnoreCase("right"))
                this.alignmentValue = Paragraph.ALIGN_RIGHT;
            else if (alignment.equalsIgnoreCase("justified"))
                this.alignmentValue = Paragraph.ALIGN_JUSTIFIED;
            else
                throw new FB2toPDFException("Invalid alignment '" + alignment + "'");
        }

        public int getAlignmentValue()
        {
            return alignmentValue;
        }
    };

    private static final class AlignmentInfoIO
        implements JsonDeserializer<AlignmentInfo>,JsonSerializer<AlignmentInfo>
    {
        public AlignmentInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            try
            {
                return new AlignmentInfo(json.getAsString());
            }
            catch(FB2toPDFException e)
            {
                throw new JsonParseException(e);
            }
        }

        public JsonElement serialize(AlignmentInfo info, Type typeOfId, JsonSerializationContext context)
        {
            return new JsonPrimitive(info.alignment);
        }
    }

    public static GsonBuilder prepare(GsonBuilder gsonBuilder)
    {
        return gsonBuilder
            .registerTypeAdapter(FontStyleInfo.class, new FontStyleInfoIO())
            .registerTypeAdapter(AlignmentInfo.class, new AlignmentInfoIO());
    }


    private transient Stylesheet stylesheet;

    private String name;
    private String baseStyle;

    private String fontFamily;
    private FontStyleInfo fontStyle;
    private Dimension fontSize;
    private Dimension leading;
    private AlignmentInfo alignment;
    private Dimension spacingBefore;
    private Dimension spacingAfter;
    private Dimension leftIndent;
    private Dimension firstLineIndent;

    private String text;
    
    public ParagraphStyle()
    {
    }

    public void setStylesheet(Stylesheet stylesheet)
    {
        this.stylesheet = stylesheet;
    }

    public Stylesheet getStylesheet()
    {
        return stylesheet;
    }

    public String getName()
    {
        return name;
    }

    private ParagraphStyle getBaseStyle()
        throws FB2toPDFException
    {
        if (baseStyle == null)
            return null;

        if (stylesheet == null)
            throw new FB2toPDFException("Stylesheet not set.");

        return stylesheet.getParagraphStyle(baseStyle);
    }
    
    private FontFamily getFontFamily()
        throws FB2toPDFException
    {
        if (stylesheet == null)
            throw new FB2toPDFException("Stylesheet not set.");

        if (fontFamily != null)
            return stylesheet.getFontFamily(fontFamily);

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getFontFamily();

        throw new FB2toPDFException("Font family for style " + name + " not defined.");
    }

    private FontStyleInfo getFontStyle()
        throws FB2toPDFException
    {
        if (fontStyle != null)
            return fontStyle;

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getFontStyle();

        // font style defaults to regular
        return new FontStyleInfo("regular");
    }
    
    private Dimension getFontSize()
        throws FB2toPDFException
    {
        if (fontSize != null)
            return fontSize;

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getFontSize();

        throw new FB2toPDFException("Font size for style " + name + " not defined.");
    }
    
    public Font getFont()
        throws FB2toPDFException
    {
        FontFamily ff = getFontFamily();

        FontStyleInfo fs = getFontStyle();

        BaseFont bf;
        if (fs.isFontItalic())
            if (fs.isFontBold())
                bf = ff.getBoldItalicFont();
            else
                bf = ff.getItalicFont();
        else
            if (fs.isFontBold())
                bf = ff.getBoldFont();
            else
                bf = ff.getRegularFont();

        return new Font(bf, getFontSize().getPoints());
    }

    private Dimension getLeadingDimension()
        throws FB2toPDFException
    {
        if (leading != null)
            return leading;
        
        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getLeadingDimension();

        // leading defaults to 1em
        return new Dimension("1em");
    }

    public float getAbsoluteLeading()
        throws FB2toPDFException
    {
        return getLeadingDimension().getPoints(getFontSize().getPoints());
    }

    public float getRelativeLeading()
        throws FB2toPDFException
    {
        return 0.0f;
    }

    public int getAlignment()
        throws FB2toPDFException
    {
        if (alignment != null)
            return alignment.getAlignmentValue();

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getAlignment();

        // alignment default
        return Paragraph.ALIGN_LEFT;
    }

    private Dimension getSpacingBeforeDimension()
        throws FB2toPDFException
    {
        if (spacingBefore != null)
            return spacingBefore;

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getSpacingBeforeDimension();

        // default value
        return new Dimension("0pt");
    }

    public float getSpacingBefore()
        throws FB2toPDFException
    {
        return getSpacingBeforeDimension().getPoints(getFontSize().getPoints());
    }

    private Dimension getSpacingAfterDimension()
        throws FB2toPDFException
    {
        if (spacingAfter != null)
            return spacingAfter;

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getSpacingAfterDimension();

        // default value
        return new Dimension("0pt");
    }

    public float getSpacingAfter()
        throws FB2toPDFException
    {
        return getSpacingAfterDimension().getPoints(getFontSize().getPoints());
    }

    private Dimension getLeftIndentDimension()
        throws FB2toPDFException
    {
        if (leftIndent != null)
            return leftIndent;

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getLeftIndentDimension();

        // default value
        return new Dimension("0pt");
    }

    public float getLeftIndent()
        throws FB2toPDFException
    {
        return getLeftIndentDimension().getPoints(getFontSize().getPoints());
    }

    private Dimension getFirstLineIndentDimension()
        throws FB2toPDFException
    {
        if (firstLineIndent != null)
            return firstLineIndent;

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null)
            return baseStyle.getFirstLineIndentDimension();

        // default value
        return new Dimension("0pt");
    }

    public float getFirstLineIndent()
        throws FB2toPDFException
    {
        return getFirstLineIndentDimension().getPoints(getFontSize().getPoints());
    }

    public String getText()
    {
        return text;
    }


    public Chunk createChunk()
        throws FB2toPDFException
    {
        Chunk chunk = new Chunk();
        chunk.setFont(getFont());
        return chunk;
    }

    public Paragraph createParagraph()
        throws FB2toPDFException
    {
        Paragraph para = new Paragraph();
        para.setLeading(getAbsoluteLeading(), getRelativeLeading());
        para.setAlignment(getAlignment());
        para.setSpacingBefore(getSpacingBefore());
        para.setSpacingAfter(getSpacingAfter());
        para.setIndentationLeft(getLeftIndent());
        para.setFirstLineIndent(getFirstLineIndent());

        return para;
    }
}
