package tech.nagual.phoenix.tools.calculator.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tech.nagual.phoenix.databinding.CalcHistoryViewBinding
import tech.nagual.phoenix.tools.calculator.activities.CalculatorActivity
import tech.nagual.phoenix.tools.calculator.helpers.CalculatorImpl
import tech.nagual.phoenix.tools.calculator.models.History
import tech.nagual.common.extensions.copyToClipboard

class HistoryAdapter(
    val activity: CalculatorActivity,
    val items: List<History>,
    val calc: CalculatorImpl,
    val itemClick: () -> Unit
) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private lateinit var binding: CalcHistoryViewBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = CalcHistoryViewBinding.inflate(activity.layoutInflater)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bindView(item)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(item: History): View {
            itemView.apply {
                binding.itemFormula.text = item.formula
                binding.itemResult.text = item.result

                setOnClickListener {
                    calc.addNumberToFormula(item.result)
                    itemClick()
                }
                setOnLongClickListener {
                    activity.baseContext.copyToClipboard(item.result)
                    true
                }
            }

            return itemView
        }
    }
}
