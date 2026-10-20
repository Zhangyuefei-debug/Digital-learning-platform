CommonError.java (通用错误信息)
这个枚举类定义了一系列标准化的错误信息。

作用: 它不是一个异常类，而是一个常量集合，用于存放可预测的业务错误信息。每个枚举项（比如 PARAMS_ERROR、QUERY_NULL）都关联一个具体的错误消息。

为什么用它: 这样做的好处是代码集中管理。你不需要在业务逻辑代码中到处硬编码错误信息，只需引用这个枚举，比如 CommonError.PARAMS_ERROR。这大大减少了出错的可能性，也方便了未来的修改和维护。



XueChengPlusException.java (自定义异常)
这是你项目的核心业务异常。

作用: 这个类继承了 RuntimeException，意味着它是一个非受检异常。这允许你在代码中抛出它而不需要强制进行 try-catch 块处理，让代码更简洁。

cast() 方法:

cast(String message): 这是一个静态方法，用于在业务逻辑中快速抛出一个 XueChengPlusException。你只需要传递一个字符串，它就会帮你创建一个异常并抛出。

cast(CommonError error): 这个静态方法更重要。它接收一个 CommonError 枚举，然后从中获取错误信息并用它来创建和抛出 XueChengPlusException。这使得在业务代码中抛出异常变得非常简单和标准化：XueChengPlusException.cast(CommonError.PARAMS_ERROR);。



RestErrorResponse.java (错误响应模型)
这个类是一个数据传输对象 (DTO)，用于封装返回给客户端的错误信息。

作用: 它定义了前端或客户端期望接收的错误响应格式。在这里，它只包含一个 errMessage 字段。

为什么需要: 前端需要一个统一的、可预测的响应体来处理所有错误。当你抛出 XueChengPlusException 后，GlobalExceptionHandler 会捕获它，然后创建一个 RestErrorResponse 对象，将错误信息放入其中，最后返回给前端。




GlobalExceptionHandler.java (全局异常处理器)
这是所有组件的核心粘合剂，负责捕获并处理所有异常。

@ControllerAdvice: 这个注解告诉 Spring 这个类是一个全局异常处理器，可以处理所有 @Controller 或 @RestController 中的异常。

@ExceptionHandler: 这个注解定义了处理特定异常类型的方法。

@ExceptionHandler(XueChengPlusException.class): 这个方法专门处理你的自定义业务异常。它捕获 XueChengPlusException，从异常中获取错误信息，并用它来构建 RestErrorResponse 返回给客户端。

@ExceptionHandler(Exception.class): 这是一个兜底的方法。它会捕获所有其他没有被前面方法处理的异常（比如 NullPointerException、ArrayIndexOutOfBoundsException 等）。它将这些未知异常统一处理为 CommonError.UNKOWN_ERROR，避免将服务器内部的详细错误信息泄露给客户端。

@ExceptionHandler(MethodArgumentNotValidException.class): 这个方法专门处理 Spring 参数校验失败的异常。当你在请求体中使用 @Valid 或 @Validated 注解时，如果验证失败，Spring 就会抛出这个异常。你的处理方法会从中提取所有验证错误信息，将它们拼接成一个字符串，并返回给客户端。

整个流程是如何串起来的？
业务层抛出异常: 在你的某个业务服务中，如果出现参数错误，你不会直接返回一个 null 或抛出一个通用的 RuntimeException，而是调用**XueChengPlusException.cast(CommonError.PARAMS_ERROR);**。

异常被传递: 这个异常会一直向上抛，直到它到达 Spring MVC 的 DispatcherServlet。

GlobalExceptionHandler 捕获: DispatcherServlet 会发现存在一个 @ControllerAdvice 类的 GlobalExceptionHandler，并且其中有一个方法可以处理 XueChengPlusException。

构建响应: GlobalExceptionHandler 中的 customException 方法被调用。它从 XueChengPlusException 中拿到**errMessage**。

返回给客户端: customException 方法用这个 errMessage 创建一个 RestErrorResponse 对象，并返回它。Spring 会将这个对象自动序列化为 JSON，并设置 HTTP 状态码（你这里使用了 @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)，所以状态码是 500），最终返回给客户端。

这个结构非常清晰，它将错误定义（CommonError）、异常封装（XueChengPlusException）、**错误响应格式（RestErrorResponse）和统一处理逻辑（GlobalExceptionHandler）**完美地分离开来，使得整个系统的异常处理变得高效且易于维护。