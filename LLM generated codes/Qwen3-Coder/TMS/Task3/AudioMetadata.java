/**
 * Container class for audio metadata
 */
public class AudioMetadata {
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int year;
    private String composer;
    private String comment;
    private String copyright;
    private int duration; // in seconds
    private int bitrate; // in kbps
    private int sampleRate; // in Hz
    private int channels;
    private String codec;
    private String format;
    
    public AudioMetadata() {
        // Default constructor
    }
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    
    public String getComposer() { return composer; }
    public void setComposer(String composer) { this.composer = composer; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public String getCopyright() { return copyright; }
    public void setCopyright(String copyright) { this.copyright = copyright; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public int getBitrate() { return bitrate; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }
    
    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
    
    public int getChannels() { return channels; }
    public void setChannels(int channels) { this.channels = channels; }
    
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    @Override
    public String toString() {
        return "AudioMetadata{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", genre='" + genre + '\'' +
                ", year=" + year +
                ", duration=" + duration + "s" +
                ", bitrate=" + bitrate + "kbps" +
                ", sampleRate=" + sampleRate + "Hz" +
                ", channels=" + channels +
                ", codec='" + codec + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}