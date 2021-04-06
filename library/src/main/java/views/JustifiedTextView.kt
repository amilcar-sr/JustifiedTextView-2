package views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import java.util.Random
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

/* ***********************************************************************

Copyright 2019 CodesGood

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*********************************************************************** */
//Created by CodesGood on 4/4/21.
class JustifiedTextView : AppCompatTextView {
    //TextView's width.
    private var viewWidth = 0

    //Cached justified sentences.
    private val sentences: MutableList<String> = ArrayList()

    //Sentence being justified.
    private val currentSentence: MutableList<String> = ArrayList()

    //Default Constructors.
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context!!, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val params = layoutParams

        //This method will attempt to justify the text if the local viewWidth has yet to be assigned
        //and the TextView hasn't wrap_content as width
        if (viewWidth == 0 && params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
            justifyText()
        }
    }

    /**
     * Attempts to justify the text on a brand new thread
     */
    private fun justifyText() {
        val executor = Executors.newFixedThreadPool(1)
        executor.submit {
            val string = text.toString()
            viewWidth = measuredWidth - (paddingLeft + paddingRight)

            //Won't justify the text if the view width is 0
            //AND! won't justify the text if it's empty.
            if (viewWidth > 0 && string.isNotEmpty()) {
                val justifiedText = getJustifiedText(string)
                if (justifiedText.isNotEmpty()) {
                    setTextAsync(justifiedText, executor)
                }
            }
        }
    }

    /**
     * Pre-computes measure of the text on a background thread and sets the text on UI the thread
     * this saves up to 90% of the work done by the TextView per Android's blog:
     *
     * https://android-developers.googleblog.com/2018/07/whats-new-for-text-in-android-p.htm
     */
    private fun setTextAsync(string: String, executor: Executor) {
        val precomputedText = PrecomputedTextCompat.getTextFuture(string, textMetricsParamsCompat, executor)
        post {
            setTextFuture(precomputedText)
        }
        sentences.clear()
        currentSentence.clear()
    }

    /**
     * Retrieves a String with appropriate spaces to justify the text in the TextView.
     *
     * @param text Text to be justified
     * @return Justified text
     */
    private fun getJustifiedText(text: String): String {
        val words = text.split(NORMAL_SPACE)

        for (word in words) {
            val containsNewLine = word.contains("\n") || word.contains("\r")
            if (fitsInSentence(word, currentSentence, true)) {
                addWord(word, containsNewLine)
            } else {
                val justifiedLine = fillCurrentSentenceGaps()
                submitSentence(justifiedLine)
                addWord(word, containsNewLine)
            }
        }

        //Making sure we add the last sentence if needed.
        if (currentSentence.isNotEmpty()) {
            submitSentence(getStringFromList(currentSentence, true))
        }

        //Returns the justified text.
        return getStringFromList(sentences, false)
    }

    /**
     * Verifies if word to be added will fit into the sentence
     *
     * @param word      Word to be added
     * @param sentence  Sentence that will receive the new word
     * @param addSpaces Specifies weather we should add spaces to validation or not
     * @return True if word fits, false otherwise.
     */
    private fun fitsInSentence(word: String, sentence: List<String>, addSpaces: Boolean): Boolean {
        val stringSentence = getStringFromList(sentence, addSpaces) + word
        val sentenceWidth = paint.measureText(stringSentence)

        return sentenceWidth < viewWidth
    }

    /**
     * Adds a word into sentence and starts a new one if "new line" is part of the string.
     *
     * @param word            Word to be added
     * @param containsNewLine Specifies if the string contains a new line
     */
    private fun addWord(word: String, containsNewLine: Boolean) {
        currentSentence.add(word)
        if (containsNewLine) {
            submitSentence(getStringFromList(currentSentence, true))
        }
    }

    /**
     * Adds a sentence to list of sentences and clears current sentence being justified.
     *
     * @param sentence Sentence to be added to list sentences
     */
    private fun submitSentence(sentence: String) {
        sentences.add(sentence)
        currentSentence.clear()
    }

    /**
     * Fills sentence with appropriate amount of spaces.
     *
     * @return String with spaces.
     */
    private fun fillCurrentSentenceGaps(): String {
        val sentenceWithSpaces: MutableList<String> = ArrayList()

        //We don't need to do this process if the sentence received is a single word.
        if (currentSentence.size > 1) {
            //We fill with normal spaces first, we can do this with confidence because "fitsInSentence"
            //already takes these spaces into account.
            for (word in currentSentence) {
                sentenceWithSpaces.add(word)
                sentenceWithSpaces.add(NORMAL_SPACE)
            }

            //Filling sentence with thin spaces.
            while (fitsInSentence(HAIR_SPACE, sentenceWithSpaces, false)) {
                //We remove 2 from the sentence size because we need to make sure we are not adding
                //spaces to the end of the line.
                sentenceWithSpaces.add(getRandomNumber(sentenceWithSpaces.size - 2), HAIR_SPACE)
            }
        }
        return getStringFromList(sentenceWithSpaces, false)
    }

    /**
     * Creates a string using the words in the list and adds spaces between words if required.
     *
     * @param strings   Strings to be merged into one
     * @param addSpaces Specifies if the method should add spaces between words.
     * @return Returns a sentence using the words in the list.
     */
    private fun getStringFromList(strings: List<String>, addSpaces: Boolean): String {
        val stringBuilder = StringBuilder()
        for (string in strings) {
            stringBuilder.append(string)
            if (addSpaces && !string.contains("\n") && !string.contains("\r")) {
                stringBuilder.append(NORMAL_SPACE)
            }
        }
        return stringBuilder.toString()
    }

    /**
     * Returns a random number, it's part of the algorithm... don't blame me.
     *
     * @param max Max number in range
     * @return Random number.
     */
    private fun getRandomNumber(max: Int): Int {
        //We add 1 to the result because we wanna prevent the logic from adding
        //spaces at the beginning of the sentence.
        return Random().nextInt(max) + 1
    }

    companion object {
        //Hair space character that will fill the space among spaces.
        private const val HAIR_SPACE = "\u200A"

        //Normal space character that will take place between words.
        private const val NORMAL_SPACE = " "
    }
}