package org.covidwatch.android.ui.temporarycontactnumbers

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.covidwatch.android.R
import org.covidwatch.android.data.TemporaryContactNumber
import org.covidwatch.android.databinding.ListItemTemporaryContactNumberBinding


class TemporaryContactNumbersAdapter() :
    PagedListAdapter<TemporaryContactNumber, TemporaryContactNumbersAdapter.ContactEventViewHolder>(
        DIFF_CALLBACK
    ) {

    override fun getItemViewType(position: Int): Int {
        return R.layout.list_item_temporary_contact_number
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactEventViewHolder {
        return ContactEventViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                viewType, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ContactEventViewHolder, position: Int) {
        val temporaryContactNumber: TemporaryContactNumber = getItem(position) ?: return
        holder.bind(temporaryContactNumber)
    }

    class ContactEventViewHolder(
        private val binding: ListItemTemporaryContactNumberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TemporaryContactNumber) {
            binding.apply {
                temporaryContactNumber = item
                executePendingBindings()
            }
        }

    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<TemporaryContactNumber>() {
            // Temporary contact number details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(
                oldConctactNumberTemporary: TemporaryContactNumber,
                newTemporaryContactNumber: TemporaryContactNumber
            ) = oldConctactNumberTemporary.bytes.contentEquals(newTemporaryContactNumber.bytes)

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldTemporaryContactNumber: TemporaryContactNumber,
                newTemporaryContactNumber: TemporaryContactNumber
            ) = oldTemporaryContactNumber == newTemporaryContactNumber
        }
    }

}
