package tech.nagual.phoenix.tools.browser.core.pages;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;


@Dao
public interface PageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPage(Page page);

    @Query("SELECT * FROM Page WHERE pid = :pid")
    Page getPage(String pid);

    @Query("UPDATE Page SET content =:content WHERE pid = :pid")
    void setContent(String pid, String content);

    @Query("UPDATE Page SET sequence = :sequence WHERE pid = :pid")
    void setSequence(String pid, long sequence);

}
