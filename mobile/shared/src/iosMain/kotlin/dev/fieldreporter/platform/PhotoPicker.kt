package dev.fieldreporter.platform

import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

/**
 * Opens the system camera or photo picker and hands back one compressed photo. The camera asks
 * for permission itself (the `NSCameraUsageDescription` text is in Info.plist). The gallery uses
 * the out-of-process picker, so it needs no permission at all.
 */
class PhotoPicker(private val compressor: PhotoCompressor) {
    // UIKit holds delegates weakly, so keep them alive until the picker reports back.
    private var cameraDelegate: CameraDelegate? = null
    private var galleryDelegate: GalleryDelegate? = null

    /** Falls back to the gallery on a device without a camera, such as the simulator. */
    fun takePhoto(onPhoto: (String) -> Unit) {
        val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(camera)) return pickFromGallery(onPhoto)

        val delegate = CameraDelegate { image -> image?.let { compressor.compress(it) }?.let(onPhoto) }
        cameraDelegate = delegate
        val picker = UIImagePickerController().apply {
            sourceType = camera
            this.delegate = delegate
        }
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }

    fun pickFromGallery(onPhoto: (String) -> Unit) {
        val delegate = GalleryDelegate { data -> data?.let { compressor.compress(it) }?.let(onPhoto) }
        galleryDelegate = delegate
        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration).apply { this.delegate = delegate }
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

private class CameraDelegate(
    private val onImage: (UIImage?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onImage(didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onImage(null)
    }
}

private class GalleryDelegate(
    private val onData: (NSData?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return onData(null)
        // Arrives on a background queue, which is fine: compressing does not touch the UI.
        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ -> onData(data) }
    }
}

/** The view controller currently in front, so a picker can be presented over it. */
internal fun topViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
    var controller = scene?.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}
